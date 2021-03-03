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
import com.google.protobuf.util.JsonFormat;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.flags.SymbolFlags;
import io.ballerina.runtime.api.types.*;
import io.ballerina.runtime.api.values.BError;
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
        } catch (BError e) {
            return e;
        } catch (Exception e) {
            return createSerdesError("Failed to generate schema for the type: " + e.getMessage(), SERDES_ERROR);
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
            String ballerinaToProtoMap = DataTypeMapper.getProtoType(type.getName());
            String messageName = ballerinaToProtoMap.toUpperCase(Locale.getDefault());

            ProtobufMessageBuilder messageBuilder = ProtobufMessage.newMessageBuilder(messageName);
            generateSchemaForPrimitive(messageBuilder, ballerinaToProtoMap, ATOMIC_FIELD_NAME, 1);

            return messageBuilder.build();
        } else if (type.getTag() == TypeTags.ARRAY_TAG) {
            ArrayType arrayType = (ArrayType) type;

            ProtobufMessageBuilder messageBuilder = ProtobufMessage.newMessageBuilder(ARRAY_BUILDER_NAME);
            generateSchemaForArray(messageBuilder, arrayType, ARRAY_FIELD_NAME, 1);

            return messageBuilder.build();
        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            RecordType recordType = (RecordType) type;

            return generateSchemaForRecord(recordType.getFields(), type.getName());
        } else {
            throw createSerdesError("Unsupported data type: " + type.getName(), SERDES_ERROR);
        }

    }

    private static void generateSchemaForPrimitive(ProtobufMessageBuilder messageBuilder, String type,
                                                   String name, int number) {
        messageBuilder.addField("optional", type, name, number);
    }

    private static void generateSchemaForArray(ProtobufMessageBuilder messageBuilder, ArrayType arrayType,
                                               String name, int number) {
        Type type = arrayType.getElementType();

        if (type.getTag() == TypeTags.UNION_TAG) {
            UnionType unionType = (UnionType) type;

            if (unionType.getMemberTypes().size() == 2) {
                for (Type member : unionType.getMemberTypes()) {
                    if (member.getTag() != TypeTags.NULL_TAG) {
                        type = member;
                        continue;
                    }
                }
            } else {
                throw createSerdesError("Unsupported data type: " + type.getName(), SERDES_ERROR);
            }
        }

        if (type.getTag() <= TypeTags.BOOLEAN_TAG) {
            String protoElementType = DataTypeMapper.getProtoType(type.getName());

            if (protoElementType.equals(BYTES)) {
                messageBuilder.addField("optional", protoElementType, name, number);
            } else {
                messageBuilder.addField("repeated", protoElementType, name, number);
            }
        } else if (type.getTag() == TypeTags.ARRAY_TAG) {
            ArrayType nestedArrayType = (ArrayType) type;
            String nestedMessageName = nestedArrayType.getElementType().getName();
            ProtobufMessageBuilder nestedMessageBuilder = ProtobufMessage.newMessageBuilder(nestedMessageName.toUpperCase(Locale.ROOT));
            generateSchemaForArray(nestedMessageBuilder, nestedArrayType, nestedMessageName, 1);

            messageBuilder.addNestedMessage(nestedMessageBuilder.build());
            messageBuilder.addField("repeated", nestedMessageName.toUpperCase(Locale.ROOT), name, number);

        } else {
            RecordType recordType = (RecordType) type;
            String[] elementNameHolder = type.getName().split(":");
            String elementType = elementNameHolder[elementNameHolder.length - 1];

            messageBuilder.addNestedMessage(generateSchemaForRecord(recordType.getFields(), recordType.getName()));
            messageBuilder.addField("repeated", elementType.toUpperCase(Locale.getDefault()), name, number);
        }

        return;
    }

    private static ProtobufMessage generateSchemaForRecord(Map<String, Field> dataTypeMap, String name) {
        ProtobufMessageBuilder messageBuilder = ProtobufMessage
                .newMessageBuilder(name.toUpperCase(Locale.getDefault()));
        int number = 1;

        for (Map.Entry<String, Field> entry : dataTypeMap.entrySet()) {
            Type fieldType = entry.getValue().getFieldType();
            String fieldName = entry.getValue().getFieldName();

            if (fieldType.getTag() == TypeTags.UNION_TAG) {
                UnionType unionType = (UnionType) fieldType;

                if (unionType.getMemberTypes().size() == 2) {
                    for (Type member : unionType.getMemberTypes()) {
                        if (member.getTag() != TypeTags.NULL_TAG) {
                            fieldType = member;
                            continue;
                        }
                    }
                } else {
                    throw createSerdesError("Unsupported data type: " + fieldType.getName(), SERDES_ERROR);
                }
            }

            if (fieldType.getClass().getSimpleName().equals(RECORD)) {
                RecordType recordType = (RecordType) fieldType;
                ProtobufMessage nestedMessage = generateSchemaForRecord(recordType.getFields(), fieldName);
                messageBuilder.addNestedMessage(nestedMessage);
                messageBuilder
                        .addField("optional", fieldName.toUpperCase(Locale.getDefault()), fieldName, number);

            } else if (fieldType.getClass().getSimpleName().equals(ARRAY)) {
                ArrayType arrayType = (ArrayType) fieldType;
                generateSchemaForArray(messageBuilder, arrayType, fieldName, number);
            } else {
                String protoFieldType = DataTypeMapper.getProtoType(fieldType.getName());
                generateSchemaForPrimitive(messageBuilder, protoFieldType, fieldName, number);
            }
            number++;
        }

        return messageBuilder.build();
    }
}
