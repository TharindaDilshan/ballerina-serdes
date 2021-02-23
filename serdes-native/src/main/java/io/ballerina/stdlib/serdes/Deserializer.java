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
import io.ballerina.runtime.api.types.RecordType;
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

    static final String BYTE = "LiteralByteString";
    static final String STRING = "String";
    static final String FLOAT = "Float";

    static final String SCHEMA_NAME = "schema";

    public static Object deserialize(BObject des, BArray encodedMessage, BTypedesc dataType) {
        Descriptor schema = (Descriptor) des.getNativeData(SCHEMA_NAME);

        DynamicMessage dynamicMessage = null;
        Object object = null;
        
        try {
            dynamicMessage = generateDynamicMessageFromBytes(schema, encodedMessage);
//            System.out.println(dynamicMessage);
            object = dynamicMessageToBallerinaType(dynamicMessage, dataType);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return object;
    }

    private static DynamicMessage generateDynamicMessageFromBytes(Descriptor schema, BArray encodedMessage) throws InvalidProtocolBufferException {
        return DynamicMessage.parseFrom(schema, encodedMessage.getBytes());
    }

    private static Object dynamicMessageToBallerinaType(DynamicMessage dynamicMessage, BTypedesc typedesc) {
        Type type = typedesc.getDescribingType();

        if (type.getTag() <= TypeTags.BOOLEAN_TAG) {
            for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : dynamicMessage.getAllFields().entrySet()) {
                return primitiveToBallerina(entry.getValue());
            }
        } else if (type.getTag() == TypeTags.ARRAY_TAG) {
            for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : dynamicMessage.getAllFields().entrySet()) {
                return arrayToBallerina(entry.getValue());
            }
        } else {
            Map<String, Object> mapObject = recordToBallerina(dynamicMessage, type);

            return ValueCreator.createRecordValue(type.getPackage(), type.getName(), mapObject);
        }

        return null;
    }

    private static Object primitiveToBallerina(Object value) {
        if (value.getClass().getSimpleName().equals(STRING)) {
            return StringUtils.fromString(value.toString());
        } else if(value.getClass().getSimpleName().equals(FLOAT)) {
            return Double.valueOf(value.toString());
        } else {
            return value;
        }
    }

    private static Object arrayToBallerina(Object value) {
        if (value.getClass().getSimpleName().equals(BYTE)) {
            ByteString byteString = (ByteString) value;

            return ValueCreator.createArrayValue(byteString.toByteArray());
        } else {
            Collection collection = (Collection) value;

            collection  = (Collection) collection.stream()
                    .map(s -> s.getClass().getSimpleName().equals(STRING) ? StringUtils.fromString(s.toString()) :
                            s.getClass().getSimpleName().equals(FLOAT) ? Double.valueOf(s.toString()) : s)
                    .collect(Collectors.toList());

            return ValueCreator.createArrayValue(
                    collection.toArray(),
                    TypeCreator.createArrayType(PredefinedTypes.TYPE_ANYDATA)
            );
        }
    }

    private static Map<String, Object> recordToBallerina(DynamicMessage dynamicMessage, Type type) {
        Map<String, Object> map = new HashMap();

        for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : dynamicMessage.getAllFields().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof DynamicMessage) {
                DynamicMessage msg = (DynamicMessage) entry.getValue();
                String recordName = entry.getKey().getName();
                Map<String, Object> nestedMap = recordToBallerina(msg, type);

                BMap<BString, Object> nestedRecord = ValueCreator.createRecordValue(
                                       type.getPackage(),
                        recordName.substring(0, 1).toUpperCase(Locale.ROOT) + recordName.substring(1),
                                       nestedMap
                );

                map.put(entry.getKey().getName(), nestedRecord);

            } else if (value.getClass().getSimpleName().equals(BYTE) || entry.getKey().isRepeated()) {
                Object handleArray = arrayToBallerina(entry.getValue());
                map.put(entry.getKey().getName(), handleArray);
            } else {
                Object handlePrimitive = primitiveToBallerina(entry.getValue());
                map.put(entry.getKey().getName(), handlePrimitive);
            }
        }


        return map;
    }
}
