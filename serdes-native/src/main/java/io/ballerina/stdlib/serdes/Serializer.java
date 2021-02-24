package io.ballerina.stdlib.serdes;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
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

    static final String SCHEMA_NAME = "schema";

    static final String ATOMIC_FIELD_NAME = "atomicField";
    static final String ARRAY_FIELD_NAME = "arrayField";

    static final String BYTE = "bytes";
    static final String STRING = "string";
    static final String INTEGER = "int32";
    static final String FLOAT = "float";
    static final String ARRAY = "ArrayValueImpl";
    static final String MESSAGE = "message";

    public static BArray serialize(BObject serializer, Object message) {
        Descriptor schema = (Descriptor) serializer.getNativeData(SCHEMA_NAME);

        DynamicMessage dynamicMessage = generateDynamicMessage(message, schema);
//        System.out.println(dynamicMessage);
        BArray bArray = ValueCreator.createArrayValue(dynamicMessage.toByteArray());

        return bArray;
    }

    private static DynamicMessage generateDynamicMessage(Object dataObject, Descriptor schema) {
        String dataType = dataObject.getClass().getSimpleName();
        String ballerinaToProtoMap = DataTypeMapper.getBallerinaToProtoMap(dataType);

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
            generateDynamicMessageForArray(newMessageFromSchema, field, dataObject);
            return  newMessageFromSchema.build();
        } else {
            return generateDynamicMessageForRecord((BMap<BString, Object>) dataObject, schema);
        }
    }

    private static void generateDynamicMessageForPrimitive(DynamicMessage.Builder messageBuilder,
                                                           FieldDescriptor field, Object value) {
        String fieldType = DataTypeMapper.getBallerinaToProtoMap(value.getClass().getSimpleName());

        if (fieldType.equals(STRING)) {
            messageBuilder.setField(field, value.toString());
        } else if (fieldType.equals(INTEGER)) {
            messageBuilder.setField(field, Integer.valueOf(value.toString()));
        } else if (fieldType.equals(FLOAT)) {
            messageBuilder.setField(field, Float.valueOf(value.toString()));
        } else {
            messageBuilder.setField(field, value);
        }
    }

    private static void generateDynamicMessageForArray(DynamicMessage.Builder messageBuilder,
                                                                 FieldDescriptor field, Object value) {
        BArray bArray = (BArray) value;
        long len = bArray.size();
        // String fieldType = DataTypeMapper.getProtoFieldType(bArray.getElementType().toString());
        String fieldType = field.getType().name().toLowerCase(Locale.ROOT);

        if (fieldType.equals(BYTE)) {
            messageBuilder.setField(field, bArray.getBytes());
            return;
        }

        for(long i = 0; i < len; i++) {
            Object element = bArray.get(i);

            if (fieldType.equals(STRING)) {
                messageBuilder.addRepeatedField(field, element.toString());
            } else if (fieldType.equals(INTEGER)) {
                messageBuilder.addRepeatedField(field, Integer.valueOf(element.toString()));
            } else if (fieldType.equals(FLOAT)) {
                messageBuilder.addRepeatedField(field, Float.valueOf(element.toString()));
            } else if (fieldType.equals(MESSAGE)) {
                // Array of records *** (revisit logic)
                for (Descriptor elementSchema : field.getContainingType().getNestedTypes()) {
                    DynamicMessage elementDynamicMessage =
                            generateDynamicMessageForRecord((BMap<BString, Object>) element, elementSchema);
                    messageBuilder.addRepeatedField(field, elementDynamicMessage);
                }
            } else {
                messageBuilder.addRepeatedField(field, element);
            }
        }
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
                String fieldName = entry.getKey().toString();
                FieldDescriptor field = messageDescriptor.findFieldByName(fieldName);

                generateDynamicMessageForArray(newMessageFromSchema, field, entry.getValue());
            }else {
                String fieldName = entry.getKey().toString();
                FieldDescriptor field = messageDescriptor.findFieldByName(fieldName);

                generateDynamicMessageForPrimitive(newMessageFromSchema, field, entry.getValue());
            }
        }
        return newMessageFromSchema.build();
    }
}
