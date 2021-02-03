package io.ballerina.stdlib.serdes;

import java.util.HashMap;
import java.util.Map;

/**
* DataTypeMapper Class.
*
*/
public class DataTypeMapper {
    private static Map<String, String> javaToProtoMap;

    public static String getProtoFieldType(String type) {
        return javaToProtoMap.get(type);
    }

    static {
        javaToProtoMap = new HashMap<>();
        javaToProtoMap.put("Double", "double");
        javaToProtoMap.put("Float", "float");
        javaToProtoMap.put("Integer", "int32");
        // javaToProtoMap.put("int64");
        // javaToProtoMap.put("uint32");
        // javaToProtoMap.put("uint64");
        // javaToProtoMap.put("sint32");
        // javaToProtoMap.put("sint64");
        // javaToProtoMap.put("fixed32");
        // javaToProtoMap.put("fixed64");
        // javaToProtoMap.put("sfixed32");
        // javaToProtoMap.put("sfixed64");
        javaToProtoMap.put("Boolean", "bool");
        javaToProtoMap.put("String", "string");
        javaToProtoMap.put("BmpStringValue", "string");
        javaToProtoMap.put("Byte", "bytes");
    }
    
}
