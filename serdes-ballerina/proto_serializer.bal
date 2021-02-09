//import ballerina/io;

class ProtoSerializer {
    private typedesc<anydata> dataType;
    private handle schema;

    public function init(typedesc<anydata> ballerinaDataType) {
        self.dataType = ballerinaDataType;
        self.schema = generateSchema(ballerinaDataType);
    }

    //public isolated function serialize(anydata data) {
    //
    //}
}
