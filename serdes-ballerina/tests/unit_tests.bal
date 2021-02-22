import ballerina/io;
import ballerina/test;

type Contact record {
    string mobile;
    //float home;
};

type Person record {
   string name;
   int age;
   byte[] img;
   Contact contact;
};

type StringArray string[];
type IntArray int[];

@test:Config{}
public function testSerialization() {
    byte[] byteArray = base16 `aeeecdefabcd12345567888822`;
    Contact phone = {mobile: "123456"};
    Person president = { name: "Joe",  age:70, img:byteArray, contact:phone };

    //io:println(president);

    // Record
    //ProtoSerializer ser = new(Person);
    ////io:println(ser);
    //byte[] encoded = ser.serialize(president);
    //ProtoDeserializer des = new(Person);
    //io:println(des.deserialize(encoded));

    // Array
    //ProtoSerializer ser = new(StringArray);
    ////io:println(ser);
    //byte[] encoded = ser.serialize(["tharinda", "dilshan"]);
    ////io:println(encoded);
    //ProtoDeserializer des = new(StringArray);
    //io:println(des.deserialize(encoded));

    // Primitive
    ProtoSerializer ser = new(float);
    byte[] encoded = ser.serialize(6.666);
    //io:println(encoded);
    ProtoDeserializer des = new(float);
    io:println(des.deserialize(encoded));
}
