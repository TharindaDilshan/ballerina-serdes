import ballerina/jballerina.java;
import ballerina/io;

type Contact record {
    string mobile;
    float home;
};

type Person record {
   string name;
   int age;
   Contact contact;
};

public function main() {

    // Person president = { name: "Joe",  age:70 };

    // string x;

    // buildSerFromType(Person);

    var str = generateSchema(Person);
    io:println(str);
}

public function generateSchema(typedesc<anydata> T) returns handle = @java:Method {
	'class: "io.ballerina.stdlib.serdes.SerdesTest"
}  external;

public function buildSerFromType(typedesc<anydata> T) {
    io:println(T);
}