import com.google.protobuf.DescriptorProtos;

/**
* Message Builder Class.
*
* @author Tharinda.
*/
public class ProtobufMessageBuilder {
    // Describes a message type
    private DescriptorProtos.DescriptorProto.Builder messageBuilder;

    // Constructor with message name as parameter
    ProtobufMessageBuilder(String messageName) {
        messageBuilder = DescriptorProtos.DescriptorProto.newBuilder();
        messageBuilder.setName(messageName);
    }

    // Add message field without default value
    public ProtobufMessageBuilder addField(String label, String type, String name, int number) {
        DescriptorProtos.FieldDescriptorProto.Label fieldLabel = ProtobufMessageField.getFieldLabel(label);
        addField(fieldLabel, type, name, number, null);
        return this;
    }

    // Add message field with default value
    public ProtobufMessageBuilder addField(String label, String type, String name, int number, String defaultValue) {
        DescriptorProtos.FieldDescriptorProto.Label fieldLabel = ProtobufMessageField.getFieldLabel(label);
        addField(fieldLabel, type, name, number, defaultValue);
        return this;
    }

    public ProtobufMessageBuilder addNestedMessage(ProtobufMessage nestedMessage) {
        messageBuilder.addNestedType(nestedMessage.getProtobufMessage());
        return this;
    }

    public ProtobufMessage build() {

        return new ProtobufMessage(messageBuilder.build());
    }

    // Add a single field to a message
    private void addField(DescriptorProtos.FieldDescriptorProto.Label label, String type, String name, int number, 
                            String defaultValue) {
        // Describes a field within a message.
        DescriptorProtos.FieldDescriptorProto.Builder messageFieldBuilder = DescriptorProtos.FieldDescriptorProto
                                                                                            .newBuilder();

        messageFieldBuilder.setLabel(label);
        messageFieldBuilder.setName(name);
        messageFieldBuilder.setNumber(number);

        DescriptorProtos.FieldDescriptorProto.Type fieldType = ProtobufMessageField.getFieldType(type);
        if (fieldType != null) {
            // Primitive types
         messageFieldBuilder.setType(fieldType);
       } else {
            // TYPE_ENUM, TYPE_MESSAGE or TYPE_GROUP
            messageFieldBuilder.setTypeName(type);
        }

        if (defaultValue != null) {
            messageFieldBuilder.setDefaultValue(defaultValue);
        }

        messageBuilder.addField(messageFieldBuilder.build());
    }
}
