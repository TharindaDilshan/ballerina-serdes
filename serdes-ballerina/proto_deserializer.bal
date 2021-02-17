//import ballerina/io;

class ProtoDeserializer {
    private typedesc<anydata> dataType;
    private handle schema;

    public function init(typedesc<anydata> ballerinaDataType) {
        self.dataType = ballerinaDataType;
        self.schema = generateSchema(ballerinaDataType);
    }

    public function deserialize(byte[] encodedMessage) returns handle {
        return deserialize(self.schema, encodedMessage);
    }
}
