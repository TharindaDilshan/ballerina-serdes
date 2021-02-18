//import ballerina/io;

class ProtoDeserializer {
    *Deserializer;

    private typedesc<anydata> dataType;

    public function init(typedesc<anydata> ballerinaDataType) {
        self.dataType = ballerinaDataType;
        generateSchema(self, ballerinaDataType);
    }

    public function deserialize(byte[] encodedMessage) returns anydata {
        return deserialize(self, encodedMessage, self.dataType);
    }
}
