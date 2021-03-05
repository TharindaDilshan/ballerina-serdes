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

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.util.Locale;
import java.util.Map;

import static io.ballerina.stdlib.serdes.Constants.SERDES_ERROR;
import static io.ballerina.stdlib.serdes.Utils.createSerdesError;

/**
 * Serializer class to create a byte array for a value.
 *
 * @return BArray.
 * @throws DescriptorValidationException
 * @exception DescriptorValidationException
 */
public class Serializer {

    static final String SCHEMA_NAME = "schema";

    static final String ATOMIC_FIELD_NAME = "atomicField";
    static final String ARRAY_FIELD_NAME = "arrayField";

    static final String BYTE = "bytes";
    static final String STRING = "string";
    static final String FLOAT = "float";
    static final String DOUBLE = "double";
    static final String ARRAY = "ArrayValueImpl";
    static final String MESSAGE = "message";

    /**
     * Creates a BArray for given data after serializing.
     *
     * @param serializer  Serializer object.
     * @param message Data that is being serialized.
     * @return Byte array of the serialized value.
     */
    public static Object serialize(BObject serializer, Object message) {
        Descriptor schema = (Descriptor) serializer.getNativeData(SCHEMA_NAME);

        DynamicMessage dynamicMessage;
        try {
            dynamicMessage = generateDynamicMessage(message, schema);
        } catch (Exception e) {
            return createSerdesError("Failed to Serialize data: " + e.getMessage(), SERDES_ERROR);
        }

        BArray bArray = ValueCreator.createArrayValue(dynamicMessage.toByteArray());

        return bArray;
    }

    private static DynamicMessage generateDynamicMessage(Object dataObject, Descriptor schema) {
        if (dataObject == null) {
            DynamicMessage.Builder newMessageFromSchema = DynamicMessage.newBuilder(schema);
            return newMessageFromSchema.build();
        }
        String dataType = dataObject.getClass().getSimpleName();
        String ballerinaToProtoMap = DataTypeMapper.getProtoTypeFromJavaType(dataType);

        if (ballerinaToProtoMap != null) {
            DynamicMessage.Builder newMessageFromSchema = DynamicMessage.newBuilder(schema);
            Descriptor messageDescriptor = newMessageFromSchema.getDescriptorForType();

            FieldDescriptor field = messageDescriptor.findFieldByName(ATOMIC_FIELD_NAME);
            generateDynamicMessageForPrimitive(newMessageFromSchema, field, dataObject);
            return  newMessageFromSchema.build();
        }

        // Non Primitive
        if (dataType.equals(ARRAY)) {
            DynamicMessage.Builder newMessageFromSchema = DynamicMessage.newBuilder(schema);
            Descriptor messageDescriptor = newMessageFromSchema.getDescriptorForType();

            FieldDescriptor field = messageDescriptor.findFieldByName(ARRAY_FIELD_NAME);
            generateDynamicMessageForArray(newMessageFromSchema, schema, field, dataObject);
            return  newMessageFromSchema.build();
        } else {
            return generateDynamicMessageForRecord((BMap<BString, Object>) dataObject, schema);
        }
    }

    private static void generateDynamicMessageForPrimitive(DynamicMessage.Builder messageBuilder,
                                                           FieldDescriptor field, Object value) {
        String fieldType = DataTypeMapper.getProtoTypeFromJavaType(value.getClass().getSimpleName());

        if (fieldType.equals(STRING)) {
            messageBuilder.setField(field, value.toString());
        } else if (fieldType.equals(FLOAT)) {
            messageBuilder.setField(field, Float.valueOf(value.toString()));
        } else if (fieldType.equals(DOUBLE)) {
            messageBuilder.setField(field, Double.valueOf(value.toString()));
        } else {
            messageBuilder.setField(field, value);
        }
    }

    private static void generateDynamicMessageForArray(DynamicMessage.Builder messageBuilder, Descriptor schema,
                                                                 FieldDescriptor field, Object value) {
        BArray bArray = (BArray) value;
        long len = bArray.size();
        Type type = bArray.getElementType();

        String fieldType = field.getType().name().toLowerCase(Locale.ROOT);

        if (type.getTag() == TypeTags.BYTE_TAG) {
            messageBuilder.setField(field, bArray.getBytes());
            return;
        }

        for(long i = 0; i < len; i++) {
            Object element = bArray.get(i);

            if (fieldType.equals(STRING)) {
                messageBuilder.addRepeatedField(field, element.toString());
            } else if (type.getTag() == TypeTags.FLOAT_TAG) {
                messageBuilder.addRepeatedField(field, Double.valueOf(element.toString()));
            } else if (type.getTag() == TypeTags.DECIMAL_TAG) {
                messageBuilder.addRepeatedField(field, Double.valueOf(element.toString()));
            } else if (type.getTag() == TypeTags.ARRAY_TAG) {
                BArray nestedArray = (BArray) element;
                Descriptor nestedSchema = schema.findNestedTypeByName(nestedArray.getElementType().getName().toUpperCase(Locale.ROOT));

                DynamicMessage.Builder nestedMessage = DynamicMessage.newBuilder(nestedSchema);
                Descriptor messageDescriptor = nestedMessage.getDescriptorForType();
                FieldDescriptor fieldDescriptor = messageDescriptor.findFieldByName(nestedArray.getElementType().getName());

                generateDynamicMessageForArray(nestedMessage, nestedSchema, fieldDescriptor, element);

                messageBuilder.addRepeatedField(field, nestedMessage.build());

            } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
                String nestedTypeName = bArray.getElementType().getName().toUpperCase(Locale.ROOT);
                Descriptor elementSchema = field.getContainingType().findNestedTypeByName(nestedTypeName);
                DynamicMessage elementDynamicMessage =
                        generateDynamicMessageForRecord((BMap<BString, Object>) element, elementSchema);

                messageBuilder.addRepeatedField(field, elementDynamicMessage);
            } else {
                messageBuilder.addRepeatedField(field, element);
            }
        }

        return;
    }

    private static DynamicMessage generateDynamicMessageForRecord(BMap<BString, Object> bMap, Descriptor schema) {
        DynamicMessage.Builder newMessageFromSchema = DynamicMessage.newBuilder(schema);
        Descriptor messageDescriptor = newMessageFromSchema.getDescriptorForType();

        for (Map.Entry<BString, Object> entry : bMap.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }

            if (entry.getValue() instanceof BMap) {
                String nestedTypeName = entry.getKey().toString().toUpperCase(Locale.getDefault());
                Descriptor subMessageDescriptor = schema.findNestedTypeByName(nestedTypeName);

                BMap<BString, Object> objToBMap = (BMap<BString, Object>) entry.getValue();
                nestedTypeName = entry.getKey().toString();
                DynamicMessage nestedMessage = generateDynamicMessageForRecord(objToBMap, subMessageDescriptor);
                newMessageFromSchema.setField(messageDescriptor.findFieldByName(nestedTypeName), nestedMessage);
            } else if (entry.getValue() instanceof BArray) {
                String fieldName = entry.getKey().toString();
                FieldDescriptor field = messageDescriptor.findFieldByName(fieldName);

                generateDynamicMessageForArray(newMessageFromSchema, schema, field, entry.getValue());
            }else {
                String fieldName = entry.getKey().toString();
                FieldDescriptor field = messageDescriptor.findFieldByName(fieldName);

                generateDynamicMessageForPrimitive(newMessageFromSchema, field, entry.getValue());
            }
        }
        return newMessageFromSchema.build();
    }
}
