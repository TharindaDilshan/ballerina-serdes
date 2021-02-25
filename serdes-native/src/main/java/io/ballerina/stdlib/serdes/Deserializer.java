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

import com.google.protobuf.*;
import com.google.protobuf.Descriptors.Descriptor;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serializer class to create Dynamic messages.
 *
 */
public class Deserializer {

    static final String BYTE = "LiteralByteString";
    static final String STRING = "String";
    static final String FLOAT = "Float";
    static final String DYNAMIC_MESSAGE = "DynamicMessage";

    static final String SCHEMA_NAME = "schema";

    public static Object deserialize(BObject des, BArray encodedMessage, BTypedesc dataType) {
        Descriptor schema = (Descriptor) des.getNativeData(SCHEMA_NAME);

        DynamicMessage dynamicMessage = null;
        Object object = null;
        
        try {
            dynamicMessage = generateDynamicMessageFromBytes(schema, encodedMessage);

            object = dynamicMessageToBallerinaType(dynamicMessage, dataType);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return object;
    }

    private static DynamicMessage generateDynamicMessageFromBytes(Descriptor schema, BArray encodedMessage) throws InvalidProtocolBufferException {
        return DynamicMessage.parseFrom(schema, encodedMessage.getBytes());
    }

    private static Object dynamicMessageToBallerinaType(DynamicMessage dynamicMessage, BTypedesc typedesc) {
        Type type = typedesc.getDescribingType();

        if (type.getTag() <= TypeTags.BOOLEAN_TAG) {
            for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : dynamicMessage.getAllFields().entrySet()) {
                return primitiveToBallerina(entry.getValue());
            }
        } else if (type.getTag() == TypeTags.ARRAY_TAG) {
            for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : dynamicMessage.getAllFields().entrySet()) {
                ArrayType arrayType = (ArrayType) type;
                Type elementType = arrayType.getElementType();

                return arrayToBallerina(entry.getValue(), elementType);
            }
        } else {
            Map<String, Object> mapObject = recordToBallerina(dynamicMessage, type);

            return ValueCreator.createRecordValue(type.getPackage(), type.getName(), mapObject);
        }

        return null;
    }

    private static Object primitiveToBallerina(Object value) {
        if (value.getClass().getSimpleName().equals(STRING)) {
            return StringUtils.fromString(value.toString());
        } else if(value.getClass().getSimpleName().equals(FLOAT)) {
            return Double.valueOf(value.toString());
        } else {
            return value;
        }
    }

    private static Object arrayToBallerina(Object value, Type type) {
        if (value.getClass().getSimpleName().equals(BYTE)) {
            ByteString byteString = (ByteString) value;

            return ValueCreator.createArrayValue(byteString.toByteArray());
        } else {
            Collection collection = (Collection) value;

            collection  = (Collection) collection.stream().map(s ->
                                s.getClass().getSimpleName().equals(STRING) ? StringUtils.fromString(s.toString()) :
                                s.getClass().getSimpleName().equals(FLOAT) ? Double.valueOf(s.toString()) :
                                s.getClass().getSimpleName().equals(DYNAMIC_MESSAGE) ?
                                                                            processRecordElement(s, type) : s)
                                .collect(Collectors.toList());

            return ValueCreator.createArrayValue(
                    collection.toArray(),
                    TypeCreator.createArrayType(PredefinedTypes.TYPE_ANYDATA)
            );
        }
    }

    private static Map<String, Object> recordToBallerina(DynamicMessage dynamicMessage, Type type) {
        Map<String, Object> map = new HashMap();

        for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : dynamicMessage.getAllFields().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof DynamicMessage) {
                DynamicMessage msg = (DynamicMessage) entry.getValue();

                Map<String, Object> nestedMap = recordToBallerina(msg, type);
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
                    Object handleArray = arrayToBallerina(entry.getValue(), elementType);

                    map.put(entry.getKey().getName(), handleArray);
                } else {
                    Object handleArray = arrayToBallerina(entry.getValue(), type);

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
            if (entry.getValue().getFieldType().getTag() == TypeTags.RECORD_TYPE_TAG && entry.getKey().equals(fieldName)) {
                return entry.getValue().getFieldType().getName();
            } else if (entry.getValue().getFieldType().getTag() == TypeTags.RECORD_TYPE_TAG) {
                return getRecordTypeName(entry.getValue().getFieldType(), fieldName);
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

    private static Object processRecordElement(Object s, Type type) {
        Map<String, Object> mapObject = recordToBallerina((DynamicMessage) s, type);

        return ValueCreator.createRecordValue(type.getPackage(), type.getName(), mapObject);
    }
}
