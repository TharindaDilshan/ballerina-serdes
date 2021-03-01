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

import com.google.protobuf.Descriptors;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BTypedesc;

import java.util.Locale;
import java.util.Map;

import static io.ballerina.stdlib.serdes.Constants.SERDES_ERROR;
import static io.ballerina.stdlib.serdes.Utils.createSerdesError;

/**
 * Generates a Protobuf schema for a given data type.
 */
public class SchemaGenerator {

    static final String SCHEMA_NAME = "schema";
    static final String SCHEMA_BUILDER_NAME = "Schema.proto";

    static final String ATOMIC_FIELD_NAME = "atomicField";
    static final String ARRAY_FIELD_NAME = "arrayField";
    static final String ARRAY_BUILDER_NAME = "ArrayBuilder";

    static final String RECORD = "BRecordType";
    static final String ARRAY = "BArrayType";
    static final String BYTES = "bytes";

    /**
     * Creates a schema for a given data type and adds to native data.
     *
     * @param serdes  Serializer or Deserializer object.
     * @param typedesc Data type that is being serialized.
     * @return {@code BError}, if there are schema generation errors, null otherwise.
     */
    public static Object generateSchema(BObject serdes, BTypedesc typedesc) {
        ProtobufMessage protobufMessage;

        try {
            protobufMessage = generateSchemaFromTypedesc(typedesc);
//            System.out.println(protobufMessage.getProtobufMessage());
        } catch (Exception e) {
            return createSerdesError("Unsupported data type: " + e.getMessage(), SERDES_ERROR);
        }

        ProtobufSchemaBuilder schemaBuilder = ProtobufSchemaBuilder.newSchemaBuilder(SCHEMA_BUILDER_NAME);
        schemaBuilder.addMessageToProtoSchema(protobufMessage);
        Descriptors.Descriptor schema = null;
        try {
            schema = schemaBuilder.build();
        } catch (Descriptors.DescriptorValidationException e) {
            return createSerdesError("Failed to generate schema: " + e.getMessage(), SERDES_ERROR);
        }
        serdes.addNativeData(SCHEMA_NAME, schema);

        return null;
    }

    private static ProtobufMessage generateSchemaFromTypedesc(BTypedesc typedesc) {
        Type type = typedesc.getDescribingType();

        if (type.getTag() <= TypeTags.BOOLEAN_TAG) {
            String ballerinaToProtoMap = DataTypeMapper.getProtoType(typedesc.getDescribingType().getName());
            String messageName = ballerinaToProtoMap.toUpperCase(Locale.getDefault());

            ProtobufMessageBuilder messageBuilder = ProtobufMessage.newMessageBuilder(messageName);
            generateSchemaForPrimitive(messageBuilder, ballerinaToProtoMap, ATOMIC_FIELD_NAME, 1);

            return messageBuilder.build();
        } else if (type.getTag() == TypeTags.ARRAY_TAG) {
            ArrayType arrayType = (ArrayType) type;

            ProtobufMessageBuilder messageBuilder = ProtobufMessage.newMessageBuilder(ARRAY_BUILDER_NAME);
            generateSchemaForArray(messageBuilder, arrayType, ARRAY_FIELD_NAME, 1);

            return messageBuilder.build();
        } else {
            RecordType recordType = (RecordType) type;
            return generateSchemaForRecord(recordType.getFields(), typedesc.getDescribingType().getName());
        }
    }

    private static void generateSchemaForPrimitive(ProtobufMessageBuilder messageBuilder, String type,
                                                   String name, int number) {
        messageBuilder.addField("required", type, name, number);
    }

    private static void generateSchemaForArray(ProtobufMessageBuilder messageBuilder, ArrayType arrayType,
                                               String name, int number) {
        // use tag instead of to string
        String elementType = DataTypeMapper.getProtoType(arrayType.getElementType().getName());

        if (elementType != null) {
            if (elementType.equals(BYTES)) {
                messageBuilder.addField("required", elementType, name, number);
            } else {
                messageBuilder.addField("repeated", elementType, name, number);
            }
        } else {
            RecordType recordType = (RecordType) arrayType.getElementType();
            String[] elementNameHolder = arrayType.getElementType().getName().split(":");
            elementType = elementNameHolder[elementNameHolder.length - 1];

            messageBuilder.addNestedMessage(generateSchemaForRecord(recordType.getFields(), recordType.getName()));
            messageBuilder.addField("repeated", elementType.toUpperCase(Locale.getDefault()), name, number);
        }
    }

    private static ProtobufMessage generateSchemaForRecord(Map<String, Field> dataTypeMap, String name) {
        ProtobufMessageBuilder messageBuilder = ProtobufMessage
                .newMessageBuilder(name.toUpperCase(Locale.getDefault()));
        int number = 1;

        for (Map.Entry<String, Field> entry : dataTypeMap.entrySet()) {
            Type fieldType = entry.getValue().getFieldType();
            String fieldName = entry.getValue().getFieldName();

            if (fieldType.getClass().getSimpleName().equals(RECORD)) {
                RecordType recordType = (RecordType) fieldType;
                ProtobufMessage nestedMessage = generateSchemaForRecord(recordType.getFields(), fieldName);
                messageBuilder.addNestedMessage(nestedMessage);
                messageBuilder
                        .addField("required", fieldName.toUpperCase(Locale.getDefault()), fieldName, number);

            } else if (fieldType.getClass().getSimpleName().equals(ARRAY)) {
                ArrayType arrayType = (ArrayType) fieldType;
                generateSchemaForArray(messageBuilder, arrayType, fieldName, number);
            } else {
                String protoFieldType = DataTypeMapper.getProtoType(fieldType.toString());
                generateSchemaForPrimitive(messageBuilder, protoFieldType, fieldName, number);
            }
            number++;
        }

        return messageBuilder.build();
    }
}
