package io.ballerina.stdlib.serdes;

import java.util.HashMap;
import java.util.Map;

/**
* DataTypeMapper Class.
*
*/
public class DataTypeMapper {
    private static Map<String, String> javaToProtoMap;
    private static Map<String, String> ballerinaToProtoMap;

    public static String getProtoFieldType(String type) {
        return javaToProtoMap.get(type);
    }

    public static String getBallerinaToProtoMap(String type) {
        return ballerinaToProtoMap.get(type);
    }

    static {
        javaToProtoMap = new HashMap<>();
        javaToProtoMap.put("Double", "float");
        javaToProtoMap.put("Float", "float");
        javaToProtoMap.put("Integer", "int32");
        javaToProtoMap.put("Long", "int32");
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
        javaToProtoMap.put("byte", "bytes");
    }

    static {
        ballerinaToProtoMap = new HashMap<>();
        ballerinaToProtoMap.put("float", "float");
        ballerinaToProtoMap.put("Long", "int32");
        ballerinaToProtoMap.put("Double", "float");
        ballerinaToProtoMap.put("integer", "int32");
        ballerinaToProtoMap.put("int", "int32");
        ballerinaToProtoMap.put("boolean", "bool");
        ballerinaToProtoMap.put("string", "string");
        ballerinaToProtoMap.put("BmpStringValue", "string");
        ballerinaToProtoMap.put("byte", "bytes");
    }
    
}
