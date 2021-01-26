import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;

import java.util.ArrayList;
import java.util.List;

public class ProtobufSchemaBuilder {
    // Describes a complete .proto file
    private DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder;
    // FileDescriptorSet containing the .proto files the compiler parses.
    private DescriptorProtos.FileDescriptorSet.Builder fileDescriptorSetBuilder;

    // Constructor
    private ProtobufSchemaBuilder(String schema) {
        fileDescriptorProtoBuilder = DescriptorProtos.FileDescriptorProto.newBuilder();
        fileDescriptorSetBuilder = DescriptorProtos.FileDescriptorSet.newBuilder();
        fileDescriptorProtoBuilder.setName(schema);
    }

    public static ProtobufSchemaBuilder newSchemaBuilder(String schema) {
        return new ProtobufSchemaBuilder(schema);
    }

    // Add message to proto schema
    public void addMessageToProtoSchema(ProtobufMessage message) {
        DescriptorProtos.DescriptorProto protobufMessage = message.getProtobufMessage();
        fileDescriptorProtoBuilder.addMessageType(protobufMessage);
    }

    // Compiles the .proto file and builds FileDescriptors
    public Descriptors.Descriptor build() throws Descriptors.DescriptorValidationException {
        DescriptorProtos.FileDescriptorSet.Builder newFileDescriptorSetBuilder = DescriptorProtos.FileDescriptorSet.newBuilder();
        newFileDescriptorSetBuilder.addFile(fileDescriptorProtoBuilder.build());
        newFileDescriptorSetBuilder.mergeFrom(fileDescriptorSetBuilder.build());
        DescriptorProtos.FileDescriptorSet fileDescriptorSet = newFileDescriptorSetBuilder.build();

        Descriptors.FileDescriptor fileDescriptor = null;
        for (DescriptorProtos.FileDescriptorProto fileDescriptorProto : fileDescriptorSet.getFileList()) {
            List<Descriptors.FileDescriptor> resolvedFileDescriptors = new ArrayList<>();

            /* Resolve import dependencies
            List<String> dependencies = fileDescriptorProto.getDependencyList();
            Map<String, Descriptors.FileDescriptor> resolvedFileDescMap = new HashMap<String, Descriptors.FileDescriptor>();

            for (String dependency : dependencies) {
                Descriptors.FileDescriptor fd = resolvedFileDescMap.get(dependency);
                if (fd != null) resolvedFileDescriptors.add(fd);
            }
            */

            Descriptors.FileDescriptor[] fileDescriptorArray = new Descriptors.FileDescriptor[resolvedFileDescriptors.size()];
//            System.out.println(fileDescriptorProto.getName());
//            System.out.println("break");
            fileDescriptor = Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, resolvedFileDescriptors.toArray(fileDescriptorArray));
        }
        Descriptors.Descriptor messageBuilder = null;
        for (Descriptors.Descriptor messageType : fileDescriptor.getMessageTypes()){
            messageBuilder = messageType;
        }

        return messageBuilder;
    }
}
