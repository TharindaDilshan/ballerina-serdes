package io.ballerina.stdlib.serdes;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BTypedesc;

import java.util.Map;

/**
 * Serializer class to create Dynamic messages.
 *
 */
public class Deserializer {
    public static Object deserialize(BObject des, BArray encodedMessage, BTypedesc dataType) {
        Descriptor schema = (Descriptor) des.getNativeData("schema");

        DynamicMessage dynamicMessage = null;
        
        try {
            dynamicMessage = generateDynamicMessageFromBytes(schema, encodedMessage);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return dynamicMessageToBallerinaType(dynamicMessage, dataType);
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
}
