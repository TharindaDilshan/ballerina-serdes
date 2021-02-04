import ballerina/jballerina.java;
import ballerina/io;

type Person record {
   string name;
   int age;
};

public function main() {

    Person president = { name: "Joe",  age:70 };

    //buildSerFromType(Person);

    io:println(Person);
}

public function generateSchema(string name) returns string = @java:Method {
	'class: "io.ballerina.stdlib.serdes.Main"
}  external;

public function buildSerFromType(typedesc<anydata> T) {
    //map<string> st = <map<string>>T;
    // io:println(T);
}