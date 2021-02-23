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
   float random;
   Contact contact;
};

type StringArray string[];
type IntArray int[];
type ByteArray byte[];
type FloatArray float[];

@test:Config{}
public function testSerialization() {
    byte[] byteArray = base16 `aeeecdefabcd12345567888822`;
    Contact phone = {mobile: "+123456"};
    Person president = { name: "Joe",  age:70, img:byteArray, random:1.666, contact:phone };

    //io:println(president);

    ProtoSerializer ser = new(Person);
    byte[] encoded = ser.serialize(president);
    ProtoDeserializer des = new(Person);
    test:assertEquals(des.deserialize(encoded), president);
    io:println(des.deserialize(encoded));
}

@test:Config{}
public function testPrimitiveFloat() {
    ProtoSerializer ser = new(float);
    byte[] encoded = ser.serialize(6.666);

    ProtoDeserializer des = new(float);
    test:assertEquals(des.deserialize(encoded), 6.666);
}

@test:Config{}
public function testPrimitiveBoolean() {
    ProtoSerializer ser = new(boolean);
    byte[] encoded = ser.serialize(true);

    ProtoDeserializer des = new(boolean);
    test:assertEquals(des.deserialize(encoded), true);
}

@test:Config{}
public function testPrimitiveString() {
    ProtoSerializer ser = new(string);
    byte[] encoded = ser.serialize("module-ballerina-serdes");

    ProtoDeserializer des = new(string);
    test:assertEquals(des.deserialize(encoded), "module-ballerina-serdes");
}

@test:Config{}
public function testPrimitiveInt() {
    ProtoSerializer ser = new(int);
    byte[] encoded = ser.serialize(666);

    ProtoDeserializer des = new(int);
    test:assertEquals(des.deserialize(encoded), 666);
}

@test:Config{}
public function testStringArray() {
    ProtoSerializer ser = new(StringArray);
    byte[] encoded = ser.serialize(["Jane", "Doe"]);
    ProtoDeserializer des = new(StringArray);
    test:assertEquals(des.deserialize(encoded), ["Jane", "Doe"]);
}

@test:Config{}
public function testIntArray() {
    ProtoSerializer ser = new(IntArray);
    byte[] encoded = ser.serialize([1, 2, 3]);
    ProtoDeserializer des = new(IntArray);
    test:assertEquals(des.deserialize(encoded), [1, 2, 3]);
}

@test:Config{}
public function testByteArray() {
    ProtoSerializer ser = new(ByteArray);
    byte[] encoded = ser.serialize(base16 `aeeecdefabcd12345567888822`);
    ProtoDeserializer des = new(ByteArray);
    test:assertEquals(des.deserialize(encoded), [174,238,205,239,171,205,18,52,85,103,136,136,34]);
}

@test:Config{}
public function testFloatArray() {
    ProtoSerializer ser = new(FloatArray);
    byte[] encoded = ser.serialize([0.123, 4.968, 3.256]);
    ProtoDeserializer des = new(FloatArray);
    test:assertEquals(des.deserialize(encoded), [0.123, 4.968, 3.256]);
}
