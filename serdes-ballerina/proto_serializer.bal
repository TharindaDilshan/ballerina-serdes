class ProtoSerializer {
    private typedesc<anydata> dataType;
    private handle schema;

    public function init(typedesc<anydata> ballerinaDataType) {
        self.dataType = ballerinaDataType;
        self.schema = generateSchema(ballerinaDataType);
    }

    public function serialize(anydata data) returns byte[] {
        return serialize(self.schema, data);
    }
}
