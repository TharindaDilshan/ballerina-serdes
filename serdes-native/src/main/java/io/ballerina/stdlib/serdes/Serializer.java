package io.ballerina.stdlib.serdes;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.DynamicMessage;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/**
 * Serializer class to create Dynamic messages.
 *
 * @return Schema.
 * @throws DescriptorValidationException
 * @exception DescriptorValidationException
 */
public class Serializer {
    public static Descriptor generateSchema(BTypedesc balType) {
        ProtobufMessage protobufMessage = generateSchemaFromTypedesc(balType);
        System.out.println(protobufMessage.getProtobufMessage());

        ProtobufSchemaBuilder schemaBuilder = ProtobufSchemaBuilder.newSchemaBuilder("Schema.proto");
        schemaBuilder.addMessageToProtoSchema(protobufMessage);
        Descriptor schema = null;
        try {
            schema = schemaBuilder.build();
        } catch (DescriptorValidationException e) {

        }

        return schema;
    }

    public static BArray serialize(Descriptor schema, Object message) {
        DynamicMessage dynamicMessage = generateDynamicMessage(message, schema);
        System.out.println(dynamicMessage);
        BArray bArray = ValueCreator.createArrayValue(dynamicMessage.toByteArray());
        return bArray;
    }

    private static ProtobufMessage generateSchemaFromTypedesc(BTypedesc typedesc) {
        Type type = typedesc.getDescribingType();

        if (type.getTag() <= TypeTags.BOOLEAN_TAG) {
            String ballerinaToProtoMap = DataTypeMapper.getBallerinaToProtoMap(typedesc.getDescribingType().getName());
            return generateSchemaForPrimitive(ballerinaToProtoMap);
        } else {
            RecordType recordType = (RecordType) type;
            return generateSchemaForRecord(recordType.getFields(), typedesc.getDescribingType().getName());
        }
    }

    private static ProtobufMessage generateSchemaForPrimitive(String type) {
        ProtobufMessageBuilder messageBuilder = ProtobufMessage
                .newMessageBuilder(type.toUpperCase(Locale.getDefault()));

        messageBuilder.addField("required", type, "atomicField", 1);
        return messageBuilder.build();
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
                String elementType = DataTypeMapper.getBallerinaToProtoMap(arrayType.getElementType().toString());
                if (elementType.equals("bytes")) {
                    messageBuilder.addField("required", elementType, fieldName, number);
                } else {
                    messageBuilder.addField("repeated", elementType, fieldName, number);
                }


            } else {
                String protoFieldType = DataTypeMapper.getBallerinaToProtoMap(fieldType.toString());
                messageBuilder.addField("required", protoFieldType, fieldName, number);
            }
            number++;
        }

        return messageBuilder.build();
    }

    private static DynamicMessage generateDynamicMessage(Object dataObject, Descriptor schema) {
        String ballerinaToProtoMap = DataTypeMapper.getBallerinaToProtoMap(dataObject.getClass().getSimpleName());

        if (ballerinaToProtoMap != null) {
            return generateDynamicMessageForPrimitive(dataObject, schema);
        } else {
            // non primitive
            return generateDynamicMessageForRecord((BMap<BString, Object>) dataObject, schema);
        }
    }

    private static DynamicMessage generateDynamicMessageForPrimitive(Object value, Descriptor schema) {
        DynamicMessage.Builder newMessageFromSchema = DynamicMessage.newBuilder(schema);
        Descriptor messageDescriptor = newMessageFromSchema.getDescriptorForType();

        Descriptors.FieldDescriptor fieldName = messageDescriptor.findFieldByName("atomicField");
        String fieldType = DataTypeMapper.getBallerinaToProtoMap(value.getClass().getSimpleName());
        if (fieldType.equals("string")) {
            newMessageFromSchema.setField(fieldName, value.toString());
        } else if (fieldType.equals("int32")) {
            newMessageFromSchema.setField(fieldName, Integer.valueOf(value.toString()));
        } else if (fieldType.equals("float")) {
            newMessageFromSchema.setField(fieldName, Float.valueOf(value.toString()));
        } else {
            newMessageFromSchema.setField(fieldName, value);
        }

        return  newMessageFromSchema.build();
    }

    private static DynamicMessage generateDynamicMessageForRecord(BMap<BString, Object> bMap, Descriptor schema) {
        DynamicMessage.Builder newMessageFromSchema = DynamicMessage.newBuilder(schema);
        Descriptor messageDescriptor = newMessageFromSchema.getDescriptorForType();

        for (Map.Entry<BString, Object> entry : bMap.entrySet()) {
            if (entry.getValue() instanceof BMap) {
                String nestedTypeName = entry.getKey().toString().toUpperCase(Locale.getDefault());
                Descriptor subMessageDescriptor = schema.findNestedTypeByName(nestedTypeName);

                BMap<BString, Object> objToBMap = (BMap<BString, Object>) entry.getValue();
                nestedTypeName = entry.getKey().toString();
                DynamicMessage nestedMessage = generateDynamicMessageForRecord(objToBMap, subMessageDescriptor);
                newMessageFromSchema.setField(messageDescriptor.findFieldByName(nestedTypeName), nestedMessage);
            } else if (entry.getValue() instanceof BArray) {
                BArray bArray = (BArray) entry.getValue();
                long len = bArray.size();
                String fieldType = DataTypeMapper.getProtoFieldType(bArray.getElementType().toString());
                String fieldName = entry.getKey().toString();
                byte[] byt = bArray.getBytes();
                System.out.println(byt);

//                System.out.println(fieldType);
                if (fieldType.equals("bytes")) {
//                    newMessageFromSchema
//                            .setField(messageDescriptor.findFieldByName(fieldName),
//                                    Byte.valueOf(bArray.getBytes()));
                    continue;
                }

                for(long i = 0; i < len; i++) {
                    Object element = bArray.get(i);

                    if (fieldType.equals("string")) {
                        newMessageFromSchema
                                .addRepeatedField(messageDescriptor.findFieldByName(fieldName), element.toString());
                    } else if (fieldType.equals("int32")) {
                        newMessageFromSchema
                                .addRepeatedField(messageDescriptor.findFieldByName(fieldName),
                                        Integer.valueOf(element.toString()));
                    } else if (fieldType.equals("float")) {
                        newMessageFromSchema
                                .addRepeatedField(messageDescriptor.findFieldByName(fieldName),
                                        Float.valueOf(element.toString()));
                    } else if (fieldType.equals("bytes")) {
                        newMessageFromSchema
                                .addRepeatedField(messageDescriptor.findFieldByName(fieldName),
                                        Byte.valueOf(element.toString()));
                    } else {
                        newMessageFromSchema
                                .addRepeatedField(messageDescriptor.findFieldByName(fieldName), element);
                    }
                }

            }else {
                String fieldType = DataTypeMapper.getProtoFieldType(entry.getValue().getClass().getSimpleName());
                String fieldName = entry.getKey().toString();
                if (fieldType.equals("string")) {
                    newMessageFromSchema
                            .setField(messageDescriptor.findFieldByName(fieldName), entry.getValue().toString());
                } else if (fieldType.equals("int32")) {
                    newMessageFromSchema
                            .setField(messageDescriptor.findFieldByName(fieldName),
                                    Integer.valueOf(entry.getValue().toString()));
                } else if (fieldType.equals("float")) {
                    newMessageFromSchema
                            .setField(messageDescriptor.findFieldByName(fieldName),
                                    Float.valueOf(entry.getValue().toString()));
                } else {
                    newMessageFromSchema
                            .setField(messageDescriptor.findFieldByName(fieldName), entry.getValue());
                }

            }
        }
        return newMessageFromSchema.build();
    }
}
