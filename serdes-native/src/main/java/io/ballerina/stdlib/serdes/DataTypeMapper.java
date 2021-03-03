/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.serdes;

import java.util.HashMap;
import java.util.Map;

/**
* Mapper class to map Java and Ballerina data types to Proto3 field types.
*
*/
public class DataTypeMapper {
    private static Map<String, String> protoTypeMapper;

    public static String getProtoType(String type) {
        return protoTypeMapper.get(type);
    }

    static {
        protoTypeMapper = new HashMap<>();
        protoTypeMapper.put("Double", "double");
        protoTypeMapper.put("Float", "double");
        protoTypeMapper.put("float", "double");
        protoTypeMapper.put("decimal", "double");
        protoTypeMapper.put("DecimalValue", "double");
        protoTypeMapper.put("Integer", "sint64");
        protoTypeMapper.put("integer", "sint64");
        protoTypeMapper.put("Long", "sint64");
        protoTypeMapper.put("int", "sint64");
        protoTypeMapper.put("Boolean", "bool");
        protoTypeMapper.put("boolean", "bool");
        protoTypeMapper.put("String", "string");
        protoTypeMapper.put("string", "string");
        protoTypeMapper.put("BmpStringValue", "string");
        protoTypeMapper.put("Byte", "bytes");
        protoTypeMapper.put("byte", "bytes");
    }
    
}
