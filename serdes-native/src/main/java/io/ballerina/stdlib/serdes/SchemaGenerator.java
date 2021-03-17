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
    static final String ARRAY_FIELD_NAME = "arrayfield";
    static final String ARRAY_BUILDER_NAME = "ArrayBuilder";
    static final String UNION_BUILDER_NAME = "UnionBuilder";
    static final String UNION_FIELD_NAME = "UnionField";
    static final String NULL_FIELD_NAME = "nullField";

    static final String BYTES = "bytes";

    static final String UNSUPPORTED_DATA_TYPE = "Unsupported data type: ";
    static final String SCHEMA_GENERATION_FAILURE = "Failed to generate schema: ";

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
//            System.out.println("-----------------------------------------------------");
        } catch (BError e) {
            return e;
        } catch (Exception e) {
            return createSerdesError(SCHEMA_GENERATION_FAILURE + e.getMessage(), SERDES_ERROR);
        }

        ProtobufSchemaBuilder schemaBuilder = ProtobufSchemaBuilder.newSchemaBuilder(SCHEMA_BUILDER_NAME);
        schemaBuilder.addMessageToProtoSchema(protobufMessage);

        Descriptors.Descriptor schema = null;
        try {
            schema = schemaBuilder.build();
        } catch (Descriptors.DescriptorValidationException e) {
            return createSerdesError(SCHEMA_GENERATION_FAILURE + e.getMessage(), SERDES_ERROR);
        }
        serdes.addNativeData(SCHEMA_NAME, schema);
        unionId = 1;

        return null;
    }

    private static ProtobufMessage generateSchemaFromTypedesc(BTypedesc typedesc) {
        Type type = typedesc.getDescribingType();

        if (type.getTag() <= TypeTags.BOOLEAN_TAG) {
            String ballerinaToProtoMap = DataTypeMapper.getProtoTypeFromTag(type.getTag());
            String messageName = ballerinaToProtoMap.toUpperCase(Locale.getDefault());

            ProtobufMessageBuilder messageBuilder = ProtobufMessage.newMessageBuilder(messageName);
            generateSchemaForPrimitive(messageBuilder, ballerinaToProtoMap, ATOMIC_FIELD_NAME, 1);

            return messageBuilder.build();
        } else if (type.getTag() == TypeTags.UNION_TAG) {
            ProtobufMessage protobufMessage = generateSchemaForUnion(typedesc.getDescribingType(), UNION_FIELD_NAME);
            ProtobufMessageBuilder messageBuilder = ProtobufMessage.newMessageBuilder(UNION_BUILDER_NAME);

            messageBuilder.addNestedMessage(protobufMessage);
            messageBuilder.addField("optional", UNION_FIELD_NAME.toUpperCase(Locale.ROOT), ATOMIC_FIELD_NAME, 1);

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
            throw createSerdesError(UNSUPPORTED_DATA_TYPE + type.getName(), SERDES_ERROR);
        }

    }

    private static void generateSchemaForPrimitive(ProtobufMessageBuilder messageBuilder, String type,
                                                   String name, int number) {
        messageBuilder.addField("optional", type, name, number);
    }

    static private int unionId = 1;
    private static void generateSchemaForArray(ProtobufMessageBuilder messageBuilder, ArrayType arrayType,
                                               String name, int number) {
        Type type = arrayType.getElementType();

        if (type.getTag() == TypeTags.UNION_TAG) {
            messageBuilder.addNestedMessage(generateSchemaForUnion(type, name + "ballerinauniontype"));
            messageBuilder.addField("repeated", name.toUpperCase(Locale.ROOT) + "BALLERINAUNIONTYPE", name, number);
        } else if (type.getTag() <= TypeTags.BOOLEAN_TAG) {
            String protoElementType = DataTypeMapper.getProtoTypeFromTag(type.getTag());

            if (protoElementType.equals(BYTES)) {
                messageBuilder.addField("optional", protoElementType, name, number);
            } else {
                messageBuilder.addField("repeated", protoElementType, name, number);
            }
        } else if (type.getTag() == TypeTags.ARRAY_TAG) {
            ArrayType nestedArrayType = (ArrayType) type;
            String nestedMessageName;
            if (nestedArrayType.getElementType().getTag() == TypeTags.UNION_TAG) {
                nestedMessageName = "unionelement" + unionId;
                unionId++;
            } else {
                nestedMessageName = nestedArrayType.getElementType().getName();
            }
//            String nestedMessageName = nestedArrayType.getElementType().getName();
//            String nestedMessageName = name;

            ProtobufMessageBuilder nestedMessageBuilder = ProtobufMessage.newMessageBuilder(nestedMessageName.toUpperCase(Locale.ROOT));
            generateSchemaForArray(nestedMessageBuilder, nestedArrayType, nestedMessageName, 1);

            messageBuilder.addNestedMessage(nestedMessageBuilder.build());
            messageBuilder.addField("repeated", nestedMessageName.toUpperCase(Locale.ROOT), name, number);

        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            RecordType recordType = (RecordType) type;

            String[] elementNameHolder = type.getName().split(":");
            String elementType = elementNameHolder[elementNameHolder.length - 1];

            messageBuilder.addNestedMessage(generateSchemaForRecord(recordType.getFields(), recordType.getName()));
            messageBuilder.addField("repeated", elementType.toUpperCase(Locale.getDefault()), name, number);
        } else {
            throw createSerdesError(UNSUPPORTED_DATA_TYPE + type.getName(), SERDES_ERROR);
        }

        return;
    }

    private static ProtobufMessage generateSchemaForRecord(Map<String, Field> dataTypeMap, String name) {
        String builderName = name.toUpperCase(Locale.getDefault());

        ProtobufMessageBuilder messageBuilder = ProtobufMessage.newMessageBuilder(builderName);

        int number = 1;
        for (Map.Entry<String, Field> entry : dataTypeMap.entrySet()) {
            Type fieldType = entry.getValue().getFieldType();
            String fieldName = entry.getValue().getFieldName();

            if (fieldType.getTag() == TypeTags.UNION_TAG) {
                fieldName = fieldName + "_ballerinauniontype";
                ProtobufMessage nestedMessage = generateSchemaForUnion(fieldType, fieldName);
                String nestedFieldType = fieldName.toUpperCase(Locale.getDefault());

                messageBuilder.addNestedMessage(nestedMessage);
                messageBuilder.addField("optional", nestedFieldType, fieldName, number);
            } else if (fieldType.getTag() == TypeTags.RECORD_TYPE_TAG) {
                RecordType recordType = (RecordType) fieldType;

                ProtobufMessage nestedMessage = generateSchemaForRecord(recordType.getFields(), fieldName);
                String nestedFieldType = fieldName.toUpperCase(Locale.getDefault());

                messageBuilder.addNestedMessage(nestedMessage);
                messageBuilder.addField("optional", nestedFieldType, fieldName, number);

            } else if (fieldType.getTag() == TypeTags.ARRAY_TAG) {
                ArrayType arrayType = (ArrayType) fieldType;

                generateSchemaForArray(messageBuilder, arrayType, fieldName, number);
            } else if (fieldType.getTag() <= TypeTags.BOOLEAN_TAG) {
                String protoFieldType = DataTypeMapper.getProtoTypeFromTag(fieldType.getTag());

                generateSchemaForPrimitive(messageBuilder, protoFieldType, fieldName, number);
            } else {
                throw createSerdesError(UNSUPPORTED_DATA_TYPE + fieldType.getName(), SERDES_ERROR);
            }
            number++;
        }

        return messageBuilder.build();
    }

    private static ProtobufMessage generateSchemaForUnion(Type type, String name) {
        String builderName = name.toUpperCase(Locale.getDefault());
        UnionType unionType = (UnionType) type;

        ProtobufMessageBuilder messageBuilder = ProtobufMessage.newMessageBuilder(builderName);

        int number = 1;
        for (Type memberType : unionType.getMemberTypes()) {
            if (memberType.getTag() <= TypeTags.BOOLEAN_TAG) {
                String ballerinaToProtoMap = DataTypeMapper.getProtoTypeFromTag(memberType.getTag());
                String fieldName = ballerinaToProtoMap + "_" + name;

                generateSchemaForPrimitive(messageBuilder, ballerinaToProtoMap, fieldName, number);
                number++;
            } else if (memberType.getTag() == TypeTags.NULL_TAG) {
                messageBuilder.addField("optional", "string", NULL_FIELD_NAME, number);
                number++;
            } else if (memberType.getTag() == TypeTags.ARRAY_TAG) {
                ArrayType arrayType = (ArrayType) memberType;
                String protoType = DataTypeMapper.getProtoTypeFromTag(arrayType.getElementType().getTag());
                if (protoType == null) {
                    protoType = arrayType.getElementType().getName();
                }
                String fieldName = protoType + "_array_" + name;

                generateSchemaForArray(messageBuilder, arrayType, fieldName, number);
                number++;
            } else if (memberType.getTag() == TypeTags.RECORD_TYPE_TAG) {
                RecordType recordType = (RecordType) memberType;
                String fieldName = memberType.getName() + "_" + name;

                String[] elementNameHolder = recordType.getName().split(":");
                String elementType = elementNameHolder[elementNameHolder.length - 1];

                messageBuilder.addNestedMessage(generateSchemaForRecord(recordType.getFields(), recordType.getName()));
                messageBuilder.addField("optional", elementType.toUpperCase(Locale.getDefault()), fieldName, number);
                number++;
            } else {
                throw createSerdesError(UNSUPPORTED_DATA_TYPE + type.getName(), SERDES_ERROR);
            }
        }

        return messageBuilder.build();
    }
}
