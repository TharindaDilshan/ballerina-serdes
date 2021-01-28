import java.util.HashMap;
import java.util.Map;

public class DataTypeMapper {
    private static Map<String, String> dataTypes;

    public static String getDataType(String type) {
        return dataTypes.get(type);
    }

    static {
        dataTypes = new HashMap<>();
        dataTypes.put("Double", "double");
        dataTypes.put("Float", "float");
        dataTypes.put("Integer", "int32");
        // dataTypes.put("int64");
        // dataTypes.put("uint32");
        // dataTypes.put("uint64");
        // dataTypes.put("sint32");
        // dataTypes.put("sint64");
        // dataTypes.put("fixed32");
        // dataTypes.put("fixed64");
        // dataTypes.put("sfixed32");
        // dataTypes.put("sfixed64");
        dataTypes.put("Boolean", "bool");
        // dataTypes.put("String", "string");
        dataTypes.put("BmpStringValue", "string");
        dataTypes.put("Byte", "bytes");
    }
    
}
