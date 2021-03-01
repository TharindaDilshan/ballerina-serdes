// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

//import ballerina/io;
import ballerina/test;

type Contact record {
    string mobile;
    string home;
};

type Street record {
    string street1;
    string street2;
};

type Address record {
    Street street;
    string country;
};

type Person record {
   string name;
   int age;
   byte[] img;
   float random;
   Contact contact;
};

type Student record {
   string name;
   int age;
   byte[] img;
   Contact[] contacts;
   Address address;
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
type CustomerTable table<map<any>>;

@test:Config{}
public function testPrimitiveFloat() returns error? {
    ProtoSerializer ser = check new(float);
    byte[] encoded = check ser.serialize(6.666);

    ProtoDeserializer des = check new(float);
    float decoded = <float>check des.deserialize(encoded);
    test:assertEquals(decoded, 6.666);
}

@test:Config{}
public function testPrimitiveBoolean() returns error? {
    ProtoSerializer ser = check new(boolean);
    byte[] encoded = check ser.serialize(true);

    ProtoDeserializer des = check new(boolean);
    boolean decoded = <boolean>check des.deserialize(encoded);
    test:assertEquals(decoded, true);
}

@test:Config{}
public function testPrimitiveString() returns error? {
    ProtoSerializer ser = check new(string);
    byte[] encoded = check ser.serialize("module-ballerina-serdes");

    ProtoDeserializer des = check new(string);
    string decoded = <string>check des.deserialize(encoded);
    test:assertEquals(decoded, "module-ballerina-serdes");
}

@test:Config{}
public function testPrimitiveInt() returns error? {
    ProtoSerializer ser = check new(int);
    byte[] encoded = check ser.serialize(666);

    ProtoDeserializer des = check new(int);
    int decoded = <int>check des.deserialize(encoded);
    test:assertEquals(decoded, 666);
}

@test:Config{}
public function testStringArray() returns error? {
    ProtoSerializer ser = check new(StringArray);
    byte[] encoded = check ser.serialize(["Jane", "Doe"]);

    ProtoDeserializer des = check new(StringArray);
    StringArray|error decoded = (check des.deserialize(encoded)).cloneWithType(StringArray);
    test:assertEquals(decoded, ["Jane", "Doe"]);
}

@test:Config{}
public function testIntArray() returns error? {
    ProtoSerializer ser = check new(IntArray);
    byte[] encoded = check ser.serialize([1, 2, 3]);

    ProtoDeserializer des = check new(IntArray);
    IntArray|error decoded = (check des.deserialize(encoded)).cloneWithType(IntArray);
    test:assertEquals(decoded, [1, 2, 3]);
}

@test:Config{}
public function testByteArray() returns error? {
    ProtoSerializer ser = check new(ByteArray);
    byte[] encoded = check ser.serialize(base16 `aeeecdefabcd12345567888822`);

    ProtoDeserializer des = check new(ByteArray);
    ByteArray decoded = <ByteArray>check des.deserialize(encoded);
    test:assertEquals(decoded, [174,238,205,239,171,205,18,52,85,103,136,136,34]);
}

@test:Config{}
public function testFloatArray() returns error? {
    ProtoSerializer ser = check new(FloatArray);
    byte[] encoded = check ser.serialize([0.123, 4.968, 3.256]);

    ProtoDeserializer des = check new(FloatArray);
    FloatArray|error decoded = (check des.deserialize(encoded)).cloneWithType(FloatArray);
    test:assertEquals(decoded, [0.123, 4.968, 3.256]);
}

@test:Config{}
public function testBooleanArray() returns error? {
    ProtoSerializer ser = check new(BoolArray);
    byte[] encoded = check ser.serialize([true, false, true, false]);

    ProtoDeserializer des = check new(BoolArray);
    BoolArray|error decoded = (check des.deserialize(encoded)).cloneWithType(BoolArray);
    test:assertEquals(decoded, [true, false, true, false]);
}

@test:Config{}
public function testRecordWithPrimitives() returns error? {

    Primitive primitiveRecord = { stringValue: "serdes", intValue: 192, floatValue: 192.168, boolValue: false };

    ProtoSerializer ser = check new(Primitive);
    byte[] encoded = check ser.serialize(primitiveRecord);

    ProtoDeserializer des = check new(Primitive);
    Primitive decoded = <Primitive>check des.deserialize(encoded);
    test:assertEquals(decoded, primitiveRecord);
}

@test:Config{}
public function testRecordWithArrays() returns error? {

    Arrays arrayRecord = {
        stringArray: ["Jane", "Doe"],
        intArray: [1, 2, 3],
        floatArray: [0.123, 4.968, 3.256],
        boolArray: [true, false, true, false],
        byteArray: base16 `aeeecdefabcd12345567888822`
    };

    ProtoSerializer ser = check new(Arrays);
    byte[] encoded = check ser.serialize(arrayRecord);

    ProtoDeserializer des = check new(Arrays);
    Arrays decoded = <Arrays>check des.deserialize(encoded);
    test:assertEquals(decoded, arrayRecord);
}

@test:Config{}
public function testNestedRecord() returns error? {
    byte[] byteArray = base16 `aeeecdefabcd12345567888822`;
    Contact phone = {mobile: "+94111111111", home: "+94777777777"};

    Person president = { name: "Joe",  age:70, img:byteArray, random:1.666, contact:phone };

    ProtoSerializer ser = check new(Person);
    byte[] encoded = check ser.serialize(president);

    ProtoDeserializer des = check new(Person);
    Person decoded = <Person>check des.deserialize(encoded);

    test:assertEquals(decoded, president);
}

@test:Config{}
public function testArrayOfRecords() returns error? {
    Contact phone1 = {mobile: "+123456", home: "789"};
    Contact phone2 = {mobile: "+456789", home: "123"};

    Contact[] contacts = [phone1, phone2];

    ProtoSerializer ser = check new(RecordArray);
    byte[] encoded = check ser.serialize(contacts);

    ProtoDeserializer des = check new(RecordArray);
    RecordArray|error decoded = (check des.deserialize(encoded)).cloneWithType(RecordArray);
    test:assertEquals(decoded, contacts);
}

@test:Config{}
public function testComplexRecord() returns error? {
    byte[] byteArray = base16 `aeeecdefabcd12345567888822`;

    Contact phone1 = {mobile: "+123456", home: "789"};
    Contact phone2 = {mobile: "+456789", home: "123"};

    Street street = { street1: "random lane", street2: "random street" };

    Address address = { street: street, country: "Sri Lanka" };

    Contact[] nums = [phone1, phone2];

    Student john = { name: "John Doe", age: 21, img: byteArray, contacts: nums, address: address };

    ProtoSerializer ser = check new(Student);
    byte[] encoded = check ser.serialize(john);

    ProtoDeserializer des = check new(Student);
    Student decoded = <Student>check des.deserialize(encoded);
    //io:println(des.deserialize(encoded));
    test:assertEquals(decoded, john);
}
