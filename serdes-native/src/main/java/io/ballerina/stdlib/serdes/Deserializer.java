package io.ballerina.stdlib.serdes;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import io.ballerina.runtime.api.values.BArray;

/**
 * Serializer class to create Dynamic messages.
 *
 */
public class Deserializer {
    public static DynamicMessage deserialize(Descriptor schema, BArray encodedMessage) {
        DynamicMessage dynamicMessage = null;
        
        try {
            dynamicMessage = generateDynamicMessageFromBytes(schema, encodedMessage);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return dynamicMessage;
    }

    private static DynamicMessage generateDynamicMessageFromBytes(Descriptor schema, BArray encodedMessage) throws InvalidProtocolBufferException {
        return DynamicMessage.parseFrom(schema, encodedMessage.getBytes());
    }
}
