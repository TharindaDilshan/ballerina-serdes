import ballerina/jballerina.java;
// import ballerina/io;

public function generateSchema(typedesc<anydata> T) returns handle = @java:Method {
	'class: "io.ballerina.stdlib.serdes.SerdesTest"
}  external;

// public function buildSerFromType(typedesc<anydata> T) {
//     io:println(T);
// }