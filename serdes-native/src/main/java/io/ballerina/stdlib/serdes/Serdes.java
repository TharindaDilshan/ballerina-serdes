package io.ballerina.stdlib.serdes;

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;

/**
 * Serdes.
 *
 */
public class Serdes {

    public String type;
    public BMap<BString, Object> typeMap = ValueCreator.createMapValue();

    public Serdes(BTypedesc balType) {
        type = balType.getDescribingType().getName();

        String ballerinaToProtoMap = DataTypeMapper.getBallerinaToProtoMap(balType.getDescribingType().getName());
        if (ballerinaToProtoMap != null) {
            typeMap.put(StringUtils.fromString("field"), ballerinaToProtoMap);
        } else {
            typeMap = balType.getDescribingType().getEmptyValue();
        }
    }

    public static Serdes generateSchema(BTypedesc balType) {
        return new Serdes(balType);
    }
}
