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
    private static Map<String, String> javaToProtoMap;
    private static Map<String, String> ballerinaToProtoMap;

    public static String getProtoFieldType(String type) {
        return javaToProtoMap.get(type);
    }

    public static String getBallerinaToProtoMap(String type) {
        return ballerinaToProtoMap.get(type);
    }

    static {
        javaToProtoMap = new HashMap<>();
        javaToProtoMap.put("Double", "float");
        javaToProtoMap.put("Float", "float");
        javaToProtoMap.put("Integer", "int32");
        javaToProtoMap.put("Long", "int32");
        // javaToProtoMap.put("int64");
        // javaToProtoMap.put("uint32");
        // javaToProtoMap.put("uint64");
        // javaToProtoMap.put("sint32");
        // javaToProtoMap.put("sint64");
        // javaToProtoMap.put("fixed32");
        // javaToProtoMap.put("fixed64");
        // javaToProtoMap.put("sfixed32");
        // javaToProtoMap.put("sfixed64");
        javaToProtoMap.put("Boolean", "bool");
        javaToProtoMap.put("String", "string");
        javaToProtoMap.put("BmpStringValue", "string");
        javaToProtoMap.put("Byte", "bytes");
        javaToProtoMap.put("byte", "bytes");
    }

    static {
        ballerinaToProtoMap = new HashMap<>();
        ballerinaToProtoMap.put("float", "float");
        ballerinaToProtoMap.put("Long", "int32");
        ballerinaToProtoMap.put("Double", "float");
        ballerinaToProtoMap.put("integer", "int32");
        ballerinaToProtoMap.put("int", "int32");
        ballerinaToProtoMap.put("Boolean", "bool");
        ballerinaToProtoMap.put("boolean", "bool");
        ballerinaToProtoMap.put("string", "string");
        ballerinaToProtoMap.put("BmpStringValue", "string");
        ballerinaToProtoMap.put("byte", "bytes");
    }
    
}
