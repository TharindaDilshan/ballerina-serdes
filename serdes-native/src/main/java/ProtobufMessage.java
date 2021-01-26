import com.google.protobuf.DescriptorProtos;

public class ProtobufMessage {
    // Describes a message type
    private DescriptorProtos.DescriptorProto protobufMessage;

    // Constructor
    public ProtobufMessage(DescriptorProtos.DescriptorProto message) {
        protobufMessage = message;
    }

    public static ProtobufMessageBuilder newMessageBuilder(String messageName) {
        return new ProtobufMessageBuilder(messageName);
    }

    public DescriptorProtos.DescriptorProto getProtobufMessage() {
        return protobufMessage;
    }

}
