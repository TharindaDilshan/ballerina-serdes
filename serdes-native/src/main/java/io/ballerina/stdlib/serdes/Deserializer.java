package io.ballerina.stdlib.serdes;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
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
    public static Object deserialize(BObject des, BArray encodedMessage, BTypedesc dataType) {
        Descriptor schema = (Descriptor) des.getNativeData("schema");

        DynamicMessage dynamicMessage = null;
        Object obj = null;
        
        try {
            dynamicMessage = generateDynamicMessageFromBytes(schema, encodedMessage);
            System.out.println(dynamicMessage);
            obj = dynamicMessageToBallerinaType(dynamicMessage, dataType);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return obj;
    }

    private static DynamicMessage generateDynamicMessageFromBytes(Descriptor schema, BArray encodedMessage) throws InvalidProtocolBufferException {
        return DynamicMessage.parseFrom(schema, encodedMessage.getBytes());
    }

    private static Object dynamicMessageToBallerinaType(DynamicMessage dynamicMessage, BTypedesc typedesc) {
        Type type = typedesc.getDescribingType();
        Object obj = null;

        if (type.getTag() <= TypeTags.BOOLEAN_TAG) {
            for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : dynamicMessage.getAllFields().entrySet()) {
                obj = primitiveToBallerina(entry.getValue());
//                System.out.println(obj);
            }
        } else if (type.getTag() == TypeTags.ARRAY_TAG) {
            for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : dynamicMessage.getAllFields().entrySet()) {
                obj = arrayToBallerina(entry.getValue());
//                System.out.println(obj);
            }
        } else {
            obj = recordToBallerina(dynamicMessage);
        }

        return obj;
    }

    private static Object primitiveToBallerina(Object value) {
        if (value.getClass().getSimpleName().equals("String")) {
            return StringUtils.fromString(value.toString());
        } else {
            return value;
        }
    }

    private static Object arrayToBallerina(Object value) {
        if (value.getClass().getSimpleName().equals("LiteralByteString")) {
            ByteString byteString = (ByteString) value;
            BArray bArrayFromByteArray = ValueCreator.createArrayValue(byteString.toByteArray());

            return bArrayFromByteArray;
        } else {
            Collection collection = (Collection) value;

            collection  = (Collection) collection.stream()
                    .map(s -> s.getClass().getSimpleName().equals("String") ? StringUtils.fromString(s.toString()) : s)
                    .collect(Collectors.toList());

            BArray bArrayFromObjectArray = ValueCreator.createArrayValue(
                    collection.toArray(), TypeCreator.createArrayType(PredefinedTypes.TYPE_ANYDATA));

            return bArrayFromObjectArray;
        }
    }

    private static Object recordToBallerina(DynamicMessage dynamicMessage) {
        BMap<BString, Object> bMap = ValueCreator.createMapValue();

        for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : dynamicMessage.getAllFields().entrySet()) {
            Object val = entry.getValue();
            System.out.println(val);
            System.out.println(entry.getValue().getClass().getSimpleName());
            System.out.println(entry.getKey().getType());
            System.out.println(entry.getKey().isRepeated());
            System.out.println("--------------------");
//            if (entry.getValue() instanceof DynamicMessage) {
//                DynamicMessage msg = (DynamicMessage) entry.getValue();
//                bMap.put(StringUtils.fromString(entry.getKey().getName()), recordToBallerina(msg));
//
//            } else {
//                Object handlePrimitive = primitiveToBallerina(entry.getValue());
//                bMap.put(StringUtils.fromString(entry.getKey().getName()), handlePrimitive);
//            }
        }

        return bMap;
    }
}
