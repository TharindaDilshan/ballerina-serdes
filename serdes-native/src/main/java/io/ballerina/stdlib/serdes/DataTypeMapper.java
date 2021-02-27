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
* DataTypeMapper Class.
*
*/
public class DataTypeMapper {
    private static Map<String, String> protoTypeMapper;

    public static String getProtoType(String type) {
        return protoTypeMapper.get(type);
    }

    static {
        protoTypeMapper = new HashMap<>();
        protoTypeMapper.put("Double", "float");
        protoTypeMapper.put("Float", "float");
        protoTypeMapper.put("float", "float");
        protoTypeMapper.put("Integer", "int32");
        protoTypeMapper.put("integer", "int32");
        protoTypeMapper.put("Long", "int32");
        protoTypeMapper.put("int", "int32");
        protoTypeMapper.put("Boolean", "bool");
        protoTypeMapper.put("boolean", "bool");
        protoTypeMapper.put("String", "string");
        protoTypeMapper.put("string", "string");
        protoTypeMapper.put("BmpStringValue", "string");
        protoTypeMapper.put("Byte", "bytes");
        protoTypeMapper.put("byte", "bytes");
    }
    
}
