package io.ballerina.stdlib.serdes;

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;

import java.util.Locale;
import java.util.Map;

/**
 * Serdes.
 *
 */
public class SerdesTest {

    public BTypedesc type;
    private static BMap<BString, Object> typeMap = ValueCreator.createMapValue();
    private ProtobufMessage protobufMessageSchema;
    // private DynamicMessage protobufDynamicMessage;

    public SerdesTest(BTypedesc balType) {
        type = balType;

        typeDescToBMap(balType);
        protobufMessageSchema = generateSchemaFromBMap(typeMap, type.getDescribingType().getName());
    }

    public static SerdesTest generateSchema(BTypedesc balType) {
        return new SerdesTest(balType);
    }

    // public static byte[] serialize(type T) {
    //     String.
    // }

    public ProtobufMessage getProtobufMessage() {
        return protobufMessageSchema;
    }

    public static void typeDescToBMap(BTypedesc balType) {
        String ballerinaToProtoMap = DataTypeMapper.getBallerinaToProtoMap(balType.getDescribingType().getName());
        if (ballerinaToProtoMap != null) {
            typeMap.put(StringUtils.fromString("field"), ballerinaToProtoMap);
        } else {
            typeMap = balType.getDescribingType().getEmptyValue();
        }
    }

    public static ProtobufMessage generateSchemaFromBMap(BMap<BString, Object> bMap, String name) {
        ProtobufMessageBuilder messageBuilder = ProtobufMessage
                                                        .newMessageBuilder(name.toUpperCase(Locale.getDefault()));
        int number = 1;

        for (Map.Entry<BString, Object> entry : bMap.entrySet()) {
            if (entry.getValue() instanceof BMap) {
                BMap<BString, Object> objToBMap = (BMap<BString, Object>) entry.getValue();
                ProtobufMessage nestedMessage = generateSchemaFromBMap(objToBMap, entry.getKey().toString());
                messageBuilder.addNestedMessage(nestedMessage);
                messageBuilder
                        .addField("required", entry.getKey().toString().toUpperCase(Locale.getDefault()),
                                entry.getKey().toString(), number);
                number++;

            } else {
                messageBuilder.addField("required", entry.getValue().toString(), entry.getKey().toString(), number);
                number++;
            }
        }
        return messageBuilder.build();
    }
}
