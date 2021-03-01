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

import ballerina/jballerina.java;

# Represents ProtoSerializer object.
class ProtoSerializer {
    *Serializer;

    # Generates a schema for a given data type.
    #
    # + ballerinaDataType - The data type of the value that needs to be serialized
    # + return - `serdes:SchemaGenerationError` if the data type is not supported else nil
    public function init(typedesc<anydata> ballerinaDataType) returns Error? {
        check generateSchema(self, ballerinaDataType);
    }

    # Serializes a given value.
    #
    # + data - The value that is being serialized
    # + return - A byte array corresponding to the encoded value
    public function serialize(anydata data) returns byte[]|Error {
        return serialize(self, data);
    }
}

public function generateSchema(Serializer | Deserializer serdes, typedesc<anydata> T) returns Error? =
@java:Method {
	'class: "io.ballerina.stdlib.serdes.SchemaGenerator"
}  external;

public function serialize(Serializer ser, anydata data) returns byte[]|Error = @java:Method {
	'class: "io.ballerina.stdlib.serdes.Serializer"
}  external;