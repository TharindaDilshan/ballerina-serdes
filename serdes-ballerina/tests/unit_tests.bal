import ballerina/io;
import ballerina/test;

type Contact record {
    string mobile;
    float home;
};

type Person record {
   string name;
   int age;
   Contact contact;
};

@test:Config{}
public function testSchemaGeneration() {
    var schema = generateSchema(Person);
    // test:assertEquals(schema, "");
    io:println(schema);
}
