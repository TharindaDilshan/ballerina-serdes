/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.serdes;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.*;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.*;

import java.util.*;
import java.util.stream.Collectors;

import static io.ballerina.stdlib.serdes.Constants.SERDES_ERROR;
import static io.ballerina.stdlib.serdes.Utils.createSerdesError;

/**
 * Deserializer class to generate an object from a byte array.
 *
 * @return Object
 */
public class Deserializer {

    static final String BYTE = "LiteralByteString";
    static final String STRING = "String";
    static final String FLOAT = "Float";
    static final String DOUBLE = "Double";
    static final String DYNAMIC_MESSAGE = "DynamicMessage";

    static final String ATOMIC_FIELD_NAME = "atomicField";
    static final String ARRAY_FIELD_NAME = "arrayField";

    static final String SCHEMA_NAME = "schema";

    /**
     * Creates an anydata object from a byte array after deserializing.
     *
     * @param des  Deserializer object.
     * @param encodedMessage Byte array corresponding to encoded data.
     * @param dataType Data type of the encoded value.
     * @return anydata object.
     */
    public static Object deserialize(BObject des, BArray encodedMessage, BTypedesc dataType) {
        Descriptor schema = (Descriptor) des.getNativeData(SCHEMA_NAME);

        DynamicMessage dynamicMessage = null;
        Object object = null;
        
        try {
            dynamicMessage = generateDynamicMessageFromBytes(schema, encodedMessage);

            object = dynamicMessageToBallerinaType(dynamicMessage, dataType, schema);
        } catch (BError e) {
            return e;
        } catch (Exception e) {
            return createSerdesError("Failed to Deserialize data: " + e.getMessage(), SERDES_ERROR);
        }

        return object;
    }

    private static DynamicMessage generateDynamicMessageFromBytes(Descriptor schema, BArray encodedMessage) throws InvalidProtocolBufferException {
        return DynamicMessage.parseFrom(schema, encodedMessage.getBytes());
    }

    private static Object dynamicMessageToBallerinaType(DynamicMessage dynamicMessage, BTypedesc typedesc,
                                                        Descriptor schema) {
        Type type = null;

        if (typedesc.getDescribingType().getTag() == TypeTags.UNION_TAG) {
            UnionType unionType = (UnionType) typedesc.getDescribingType();

            if (unionType.getMemberTypes().size() == 2) {
                for (Type elementType : unionType.getMemberTypes()) {
                    if (elementType.getTag() != TypeTags.NULL_TAG) {
                        type = elementType;
                        continue;
                    }
                }
            } else {
                throw createSerdesError("Unsupported data type: " + typedesc.getDescribingType().getName(), SERDES_ERROR);
            }
        } else {
            type = typedesc.getDescribingType();
        }

        if (type.getTag() <= TypeTags.BOOLEAN_TAG) {
            FieldDescriptor fieldDescriptor = schema.findFieldByName(ATOMIC_FIELD_NAME);

            return primitiveToBallerina(dynamicMessage.getField(fieldDescriptor));
        } else if (type.getTag() == TypeTags.ARRAY_TAG) {
            ArrayType arrayType = (ArrayType) type;
            Type elementType = arrayType.getElementType();
            FieldDescriptor fieldDescriptor = schema.findFieldByName(ARRAY_FIELD_NAME);
            schema = fieldDescriptor.getContainingType();

            return arrayToBallerina(dynamicMessage.getField(fieldDescriptor), elementType, schema);
        } else {
            Map<String, Object> mapObject = recordToBallerina(dynamicMessage, type, schema);

            return ValueCreator.createRecordValue(type.getPackage(), type.getName(), mapObject);
        }
    }

    private static Object primitiveToBallerina(Object value) {

        if (value.getClass().getSimpleName().equals(STRING)) {
            if (value.toString().equals("")) {
                return null;
            }

            return StringUtils.fromString(value.toString());
        } else if(value.getClass().getSimpleName().equals(FLOAT)) {
            if (Double.valueOf(value.toString()) == 0.0) {
                return null;
            }

            return Double.valueOf(value.toString());
        } else if(value.getClass().getSimpleName().equals(DOUBLE)) {
            if (Double.valueOf(value.toString()) == 0.0) {
                return null;
            }

            return ValueCreator.createDecimalValue(value.toString());
        } else {
            return value;
        }
    }

    private static Object arrayToBallerina(Object value, Type type, Descriptor schema) {
        if (value.getClass().getSimpleName().equals(BYTE)) {
            ByteString byteString = (ByteString) value;

            return ValueCreator.createArrayValue(byteString.toByteArray());
        } else {
            Collection collection = (Collection) value;

            BArray bArray = ValueCreator.createArrayValue(TypeCreator.createArrayType(type));

            for (Iterator it = collection.iterator(); it.hasNext(); ) {
                Object element = it.next();

                if (type.getTag() == TypeTags.STRING_TAG) {
                    bArray.append(StringUtils.fromString(element.toString()));
                } else if (type.getTag() == TypeTags.FLOAT_TAG) {
                    bArray.append(Double.valueOf(element.toString()));
                } else if (type.getTag() == TypeTags.ARRAY_TAG) {
                    ArrayType arrayType = (ArrayType) type;
                    Type elementType = arrayType.getElementType();

                    Descriptor nestedSchema = schema.findNestedTypeByName(elementType.getName().toUpperCase(Locale.ROOT));
                    DynamicMessage nestedDynamicMessage = (DynamicMessage) element;
                    FieldDescriptor fieldDescriptor = nestedSchema.findFieldByName(elementType.getName());

                    BArray nestedArray = (BArray) arrayToBallerina(nestedDynamicMessage.getField(fieldDescriptor),
                                                                   elementType, schema);
                    bArray.append(nestedArray);
                } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
                    Map<String, Object> mapObject = recordToBallerina((DynamicMessage) element, type, schema);

                    bArray.append(ValueCreator.createRecordValue(type.getPackage(), type.getName(), mapObject));
                } else {
                    bArray.append(element);
                }
            }

            return bArray;
        }
    }

    private static Map<String, Object> recordToBallerina(DynamicMessage dynamicMessage, Type type, Descriptor schema) {
        Map<String, Object> map = new HashMap();

        for (Map.Entry<FieldDescriptor, Object> entry : dynamicMessage.getAllFields().entrySet()) {
            Object value = entry.getValue();

            if (value instanceof DynamicMessage) {
                DynamicMessage msg = (DynamicMessage) entry.getValue();

                Map<String, Object> nestedMap = recordToBallerina(msg, type, schema);
                String recordTypeName = getRecordTypeName(type, entry.getKey().getName());

                BMap<BString, Object> nestedRecord = ValueCreator.createRecordValue(
                                        type.getPackage(),
                                        recordTypeName,
                                        nestedMap
                );

                map.put(entry.getKey().getName(), nestedRecord);

            } else if (value.getClass().getSimpleName().equals(BYTE) || entry.getKey().isRepeated()) {
                if (!value.getClass().getSimpleName().equals(BYTE)) {
                    Type elementType = getArrayElementType(type, entry.getKey().getName());
                    Object handleArray = arrayToBallerina(entry.getValue(), elementType, schema);

                    map.put(entry.getKey().getName(), handleArray);
                } else {
                    Object handleArray = arrayToBallerina(entry.getValue(), type, schema);

                    map.put(entry.getKey().getName(), handleArray);
                }

            } else {
                Object handlePrimitive = primitiveToBallerina(entry.getValue());

                map.put(entry.getKey().getName(), handlePrimitive);
            }
        }

        return map;
    }

    private static String getRecordTypeName(Type type, String fieldName) {
        RecordType recordType = (RecordType) type;

        for (Map.Entry<String, Field> entry: recordType.getFields().entrySet()) {
            Type fieldType = entry.getValue().getFieldType();

            if (fieldType.getTag() == TypeTags.UNION_TAG) {
                UnionType unionType = (UnionType) fieldType;

                if (unionType.getMemberTypes().size() == 2) {
                    for (Type elementType : unionType.getMemberTypes()) {
                        if (elementType.getTag() != TypeTags.NULL_TAG) {
                            fieldType = elementType;
                            continue;
                        }
                    }
                } else {
                    throw createSerdesError("Unsupported data type: " + fieldType.getName(), SERDES_ERROR);
                }
            }

            if (fieldType.getTag() == TypeTags.RECORD_TYPE_TAG && entry.getKey().equals(fieldName)) {
                return fieldType.getName();
            } else if (fieldType.getTag() == TypeTags.RECORD_TYPE_TAG) {
                return getRecordTypeName(fieldType, fieldName);
            } else {
                continue;
            }
        }

        return null;
    }

    private static Type getArrayElementType(Type type, String fieldName) {
        RecordType recordType = (RecordType) type;

        for (Map.Entry<String, Field> entry: recordType.getFields().entrySet()) {
            if (entry.getValue().getFieldType().getTag() == TypeTags.ARRAY_TAG && entry.getKey().equals(fieldName)) {
                ArrayType arrayType = (ArrayType) entry.getValue().getFieldType();

                return arrayType.getElementType();
            } else if (entry.getValue().getFieldType().getTag() == TypeTags.RECORD_TYPE_TAG) {
                return getArrayElementType(entry.getValue().getFieldType(), fieldName);
            } else {
                continue;
            }
        }

        return null;
    }

}
