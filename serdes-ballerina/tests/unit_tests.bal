import ballerina/io;
import ballerina/test;

type Contact record {
    string mobile;
    string home;
};

type Person record {
   string name;
   int age;
   byte[] img;
   float random;
   Contact contact;
};

type Primitive record {
    string stringValue;
    int intValue;
    float floatValue;
    boolean boolValue;
};

type Arrays record {
    string[] stringArray;
    int[] intArray;
    float[] floatArray;
    boolean[] boolArray;
    byte[] byteArray;
};

type StringArray string[];
type IntArray int[];
type ByteArray byte[];
type FloatArray float[];
type BoolArray boolean[];

type RecordArray Contact[];

@test:Config{}
public function testSerialization() {
    Contact phone1 = {mobile: "+123456", home: "789"};
    Contact phone2 = {mobile: "+456789", home: "123"};

    Contact[] contacts = [phone1, phone2];

    ProtoSerializer ser = new(RecordArray);
    byte[] encoded = ser.serialize(contacts);
    //
    ProtoDeserializer des = new(RecordArray);
    //test:assertEquals(des.deserialize(encoded), president);
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

@test:Config{}
public function testBooleanArray() {
    ProtoSerializer ser = new(BoolArray);
    byte[] encoded = ser.serialize([true, false, true, false]);
    ProtoDeserializer des = new(BoolArray);
    test:assertEquals(des.deserialize(encoded), [true, false, true, false]);
}

@test:Config{}
public function testRecordWithPrimitives() {

    Primitive primitiveRecord = { stringValue: "serdes", intValue: 192, floatValue: 192.168, boolValue: false };

    ProtoSerializer ser = new(Primitive);
    byte[] encoded = ser.serialize(primitiveRecord);

    ProtoDeserializer des = new(Primitive);
    test:assertEquals(des.deserialize(encoded), primitiveRecord);
}

@test:Config{}
public function testRecordWithArrays() {

    Arrays arrayRecord = {
        stringArray: ["Jane", "Doe"],
        intArray: [1, 2, 3],
        floatArray: [0.123, 4.968, 3.256],
        boolArray: [true, false, true, false],
        byteArray: base16 `aeeecdefabcd12345567888822`
    };

    ProtoSerializer ser = new(Arrays);
    byte[] encoded = ser.serialize(arrayRecord);

    ProtoDeserializer des = new(Arrays);
    test:assertEquals(des.deserialize(encoded), arrayRecord);
}

@test:Config{}
public function testNestedRecord() {
    byte[] byteArray = base16 `aeeecdefabcd12345567888822`;
    Contact phone = {mobile: "+94111111111", home: "+94777777777"};

    Person president = { name: "Joe",  age:70, img:byteArray, random:1.666, contact:phone };

    ProtoSerializer ser = new(Person);
    byte[] encoded = ser.serialize(president);

    ProtoDeserializer des = new(Person);
    Person deserializedPresident = <Person> des.deserialize(encoded);

    test:assertEquals(deserializedPresident, president);
}