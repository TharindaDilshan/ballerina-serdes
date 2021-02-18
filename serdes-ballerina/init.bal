import ballerina/jballerina.java;

public function generateSchema(Serializer | Deserializer serdes, typedesc<anydata> T) = @java:Method {
	'class: "io.ballerina.stdlib.serdes.SchemaGenerator"
}  external;

public function serialize(Serializer ser, anydata data) returns byte[] = @java:Method {
	'class: "io.ballerina.stdlib.serdes.Serializer"
}  external;

public function deserialize(Deserializer des, byte[] encodedMessage, typedesc<anydata> T) returns anydata =
@java:Method {
    'class: "io.ballerina.stdlib.serdes.Deserializer"
}  external;