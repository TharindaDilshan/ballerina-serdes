import com.google.protobuf.DescriptorProtos;

import java.util.HashMap;
import java.util.Map;

/**
* Message Field Definitions.
*
* @author Tharinda.
*/
public class ProtobufMessageField {

    private static Map<String, DescriptorProtos.FieldDescriptorProto.Type> fieldTypes;
    private static Map<String, DescriptorProtos.FieldDescriptorProto.Label> fieldLabels;

    public static DescriptorProtos.FieldDescriptorProto.Type getFieldType(String type) {
        return fieldTypes.get(type);
    }

    public static DescriptorProtos.FieldDescriptorProto.Label getFieldLabel(String type) {
        return fieldLabels.get(type);
    }

    static {
        fieldTypes = new HashMap<>();
        fieldTypes.put("double", DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE);
        fieldTypes.put("float", DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT);
        fieldTypes.put("int32", DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32);
        fieldTypes.put("int64", DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64);
        fieldTypes.put("uint32", DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32);
        fieldTypes.put("uint64", DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64);
        fieldTypes.put("sint32", DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32);
        fieldTypes.put("sint64", DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64);
        fieldTypes.put("fixed32", DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32);
        fieldTypes.put("fixed64", DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64);
        fieldTypes.put("sfixed32", DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED32);
        fieldTypes.put("sfixed64", DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED64);
        fieldTypes.put("bool", DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL);
        fieldTypes.put("string", DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING);
        fieldTypes.put("bytes", DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES);
//        fieldTypes.put("enum", DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM);
//        fieldTypes.put("message", DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE);
//        fieldTypes.put("group", DescriptorProtos.FieldDescriptorProto.Type.TYPE_GROUP);

        fieldLabels = new HashMap<>();
        fieldLabels.put("optional", DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL);
        fieldLabels.put("required", DescriptorProtos.FieldDescriptorProto.Label.LABEL_REQUIRED);
        fieldLabels.put("repeated", DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED);
    }
}
