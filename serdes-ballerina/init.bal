import ballerina/jballerina.java;

public function generateSchema(typedesc<anydata> T) returns handle = @java:Method {
	'class: "io.ballerina.stdlib.serdes.SchemaGenerator"
}  external;

public function serialize(handle schema, anydata data) returns byte[] = @java:Method {
	'class: "io.ballerina.stdlib.serdes.Serializer"
}  external;

public function deserialize(handle schema, byte[] encodedMessage) returns handle = @java:Method {
    'class: "io.ballerina.stdlib.serdes.Deserializer"
}  external;