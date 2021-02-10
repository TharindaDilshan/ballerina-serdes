package io.ballerina.stdlib.serdes;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.DynamicMessage;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;

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
        String messageBuilderName = balType.getDescribingType().getName();
        BMap<BString, Object> bMap = typeDescToBMap(balType);
        ProtobufMessage protobufMessage = generateSchemaFromBMap(bMap, messageBuilderName);

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
        DynamicMessage dynamicMessage = generateDynamicMessage(objectToBMap(message), schema);
        BArray bArray = ValueCreator.createArrayValue(dynamicMessage.toByteArray());
        return bArray;
    }

    private static BMap<BString, Object> typeDescToBMap(BTypedesc balType) {
        BMap<BString, Object> typeMap = ValueCreator.createMapValue();
        String ballerinaToProtoMap = DataTypeMapper.getBallerinaToProtoMap(balType.getDescribingType().getName());

//        Type type = balType.getDescribingType();
//        if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
//            RecordType recordType = (RecordType) type;
//            TypeTa
//        }

        if (ballerinaToProtoMap != null) {
            typeMap.put(StringUtils.fromString("field"), ballerinaToProtoMap);
        } else {
            // non primitive
            typeMap = balType.getDescribingType().getEmptyValue();
        }

        return typeMap;
    }

    private static ProtobufMessage generateSchemaFromBMap(BMap<BString, Object> bMap, String name) {
        ProtobufMessageBuilder messageBuilder = ProtobufMessage
                .newMessageBuilder(name.toUpperCase(Locale.getDefault()));
        int number = 1;

        for (Map.Entry<BString, Object> entry : bMap.entrySet()) {
            if (entry.getValue() instanceof BMap) {
                BMap<BString, Object> objToBMap = (BMap<BString, Object>) entry.getValue();
                ProtobufMessage nestedMessage = generateSchemaFromBMap(objToBMap, entry.getKey().toString());
                messageBuilder.addNestedMessage(nestedMessage);
                String fieldType = entry.getKey().toString().toUpperCase(Locale.getDefault());
                String fieldName = entry.getKey().toString();
                messageBuilder
                        .addField("required", fieldType, fieldName, number);

            } else {
                String fieldType = DataTypeMapper.getProtoFieldType(entry.getValue().getClass().getSimpleName());
                String fieldName = entry.getKey().toString();
                messageBuilder.addField("required", fieldType, fieldName, number);
            }
            number++;
        }
        return messageBuilder.build();
    }

    private static BMap<BString, Object> objectToBMap(Object object) {
        BMap<BString, Object> typeMap = ValueCreator.createMapValue();
        String ballerinaToProtoMap = DataTypeMapper.getBallerinaToProtoMap(object.getClass().getSimpleName());

        if (ballerinaToProtoMap != null) {
            if (ballerinaToProtoMap.equals("string")) {
                typeMap.put(StringUtils.fromString("field"), object.toString());
            } else {
                typeMap.put(StringUtils.fromString("field"), object);
            }
        } else {
            // non primitive
            typeMap = (BMap<BString, Object>)object;
        }

        return typeMap;
    }

    private static DynamicMessage generateDynamicMessage(BMap<BString, Object> bMap, Descriptor schema) {

        DynamicMessage.Builder newMessageFromSchema = DynamicMessage.newBuilder(schema);
        Descriptor messageDescriptor = newMessageFromSchema.getDescriptorForType();

        for (Map.Entry<BString, Object> entry : bMap.entrySet()) {
            if (entry.getValue() instanceof BMap) {
                String nestedTypeName = entry.getKey().toString().toUpperCase(Locale.getDefault());
                Descriptor subMessageDescriptor = schema.findNestedTypeByName(nestedTypeName);

                BMap<BString, Object> objToBMap = (BMap<BString, Object>) entry.getValue();
                nestedTypeName = entry.getKey().toString();
                DynamicMessage nestedMessage = generateDynamicMessage(objToBMap, subMessageDescriptor);
                newMessageFromSchema.setField(messageDescriptor.findFieldByName(nestedTypeName), nestedMessage);
            } else {
                String fieldType = DataTypeMapper.getProtoFieldType(entry.getValue().getClass().getSimpleName());
                String fieldName = entry.getKey().toString();
                if (fieldType.equals("string")) {
                    newMessageFromSchema.setField(messageDescriptor.findFieldByName(fieldName), entry.getValue().toString());
                } else if (fieldType.equals("int32")) {
                    newMessageFromSchema.setField(messageDescriptor.findFieldByName(fieldName), Integer.valueOf(entry.getValue().toString()));
                } else {
                    newMessageFromSchema.setField(messageDescriptor.findFieldByName(fieldName), entry.getValue());
                }

            }
        }
        return newMessageFromSchema.build();
    }
}
