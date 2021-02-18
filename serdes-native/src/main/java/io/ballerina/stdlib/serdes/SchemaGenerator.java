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

/**
 * Schema generator class.
 *
 * @return Schema.
 */
public class SchemaGenerator {

    public static void generateSchema(BObject serializer, BTypedesc balType) {
        ProtobufMessage protobufMessage = generateSchemaFromTypedesc(balType);
//        System.out.println(protobufMessage.getProtobufMessage());

        ProtobufSchemaBuilder schemaBuilder = ProtobufSchemaBuilder.newSchemaBuilder("Schema.proto");
        schemaBuilder.addMessageToProtoSchema(protobufMessage);
        Descriptors.Descriptor schema = null;
        try {
            schema = schemaBuilder.build();
        } catch (Descriptors.DescriptorValidationException e) {

        }
        serializer.addNativeData("schema", schema);
    }

    private static ProtobufMessage generateSchemaFromTypedesc(BTypedesc typedesc) {
        Type type = typedesc.getDescribingType();

        if (type.getTag() <= TypeTags.BOOLEAN_TAG) {
            String ballerinaToProtoMap = DataTypeMapper.getBallerinaToProtoMap(typedesc.getDescribingType().getName());
            String messageName = ballerinaToProtoMap.toUpperCase(Locale.getDefault());

            ProtobufMessageBuilder messageBuilder = ProtobufMessage.newMessageBuilder(messageName);
            generateSchemaForPrimitive(messageBuilder, ballerinaToProtoMap, "atomicField", 1);

            return messageBuilder.build();
        } else if (type.getTag() == TypeTags.ARRAY_TAG) {
            ArrayType arrayType = (ArrayType) type;

            ProtobufMessageBuilder messageBuilder = ProtobufMessage.newMessageBuilder("ArrayBuilder");
            generateSchemaForArray(messageBuilder, arrayType, "arrayField", 1);

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
        String elementType = DataTypeMapper.getBallerinaToProtoMap(arrayType.getElementType().toString());

        if (elementType.equals("bytes")) {
            messageBuilder.addField("required", elementType, name, number);
        } else {
            messageBuilder.addField("repeated", elementType, name, number);
        }
    }

    private static ProtobufMessage generateSchemaForRecord(Map<String, Field> dataTypeMap, String name) {
        ProtobufMessageBuilder messageBuilder = ProtobufMessage
                .newMessageBuilder(name.toUpperCase(Locale.getDefault()));
        int number = 1;

        for (Map.Entry<String, Field> entry : dataTypeMap.entrySet()) {
            Type fieldType = entry.getValue().getFieldType();
            String fieldName = entry.getValue().getFieldName();

            if (fieldType.getClass().getSimpleName().equals("BRecordType")) {
                RecordType recordType = (RecordType) fieldType;
                ProtobufMessage nestedMessage = generateSchemaForRecord(recordType.getFields(), fieldName);
                messageBuilder.addNestedMessage(nestedMessage);
                messageBuilder
                        .addField("required", fieldName.toUpperCase(Locale.getDefault()), fieldName, number);

            } else if (fieldType.getClass().getSimpleName().equals("BArrayType")) {
                ArrayType arrayType = (ArrayType) fieldType;
                generateSchemaForArray(messageBuilder, arrayType, fieldName, number);
            } else {
                String protoFieldType = DataTypeMapper.getBallerinaToProtoMap(fieldType.toString());
                generateSchemaForPrimitive(messageBuilder, protoFieldType, fieldName, number);
            }
            number++;
        }

        return messageBuilder.build();
    }
}
