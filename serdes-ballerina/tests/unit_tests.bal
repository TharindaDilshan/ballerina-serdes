import ballerina/io;
import ballerina/test;

type Contact record {
    string mobile;
    float home;
};

type Person record {
   string name;
   int age;
   int[] numbers;
   Contact contact;
};

@test:Config{}
public function testSchemaGeneration() {
    Contact phone = {mobile: "123456", home: 1.234};
    Person president = { name: "Joe",  age:70, numbers:[2,4], contact:phone };

    ProtoSerializer ser = new(Person);
    byte[] encoded = ser.serialize(president);

    io:println(encoded);
}
