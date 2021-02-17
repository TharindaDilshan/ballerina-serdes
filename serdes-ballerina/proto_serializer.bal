class ProtoSerializer {
    *Serializer;

    public function init(typedesc<anydata> ballerinaDataType) {
        generateSchema(self, ballerinaDataType);
    }

    public function serialize(anydata data) returns byte[] {
        return serialize(self, data);
    }
}
