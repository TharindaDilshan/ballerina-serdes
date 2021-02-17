package io.ballerina.stdlib.serdes;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

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
    public static BArray serialize(Descriptor schema, Object message) {
        DynamicMessage dynamicMessage = generateDynamicMessage(message, schema);
//        System.out.println(dynamicMessage);
        BArray bArray = ValueCreator.createArrayValue(dynamicMessage.toByteArray());
        try {
            System.out.println(DynamicMessage.parseFrom(schema, dynamicMessage.toByteArray()));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return bArray;
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

                if (fieldType.equals("bytes")) {
                    newMessageFromSchema.setField(messageDescriptor.findFieldByName(fieldName), bArray.getBytes());
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
