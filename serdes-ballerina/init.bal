import ballerina/jballerina.java;

public function generateSchema(typedesc<anydata> T) returns handle = @java:Method {
	'class: "io.ballerina.stdlib.serdes.Serializer"
}  external;

public function serialize(handle schema, anydata data) = @java:Method {
	'class: "io.ballerina.stdlib.serdes.Serializer"
}  external;