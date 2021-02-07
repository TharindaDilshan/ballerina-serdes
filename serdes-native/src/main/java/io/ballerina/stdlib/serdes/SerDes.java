package io.ballerina.stdlib.serdes;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.util.Locale;
import java.util.Map;

/**
 * SerDes class to create Dynamic messages.
 *
 * @return None.
 * @throws DescriptorValidationException
 * @exception DescriptorValidationException
 */
public class SerDes {
    public static void main(String[] args) throws DescriptorValidationException {
        // serdesFunction();
        BMap<BString, Object> person = ValueCreator.createMapValue();
        person.put(StringUtils.fromString("name"), "Tharinda");

        BMap<BString, Object> contact = ValueCreator.createMapValue();

        BMap<BString, Object> phone = ValueCreator.createMapValue();
        phone.put(StringUtils.fromString("mobile"), 123456);
        phone.put(StringUtils.fromString("home"), 6666666);

        BMap<BString, Object> address = ValueCreator.createMapValue();
        address.put(StringUtils.fromString("street1"), "abcd");
        address.put(StringUtils.fromString("street2"), "qwerty");

        contact.put(StringUtils.fromString("Phone"), phone);
        contact.put(StringUtils.fromString("Address"), address);
        person.put(StringUtils.fromString("Contact"), contact);


        ProtobufMessage protobufMessage = generateSchema(person, "Person");

        ProtobufSchemaBuilder schemaBuilder = ProtobufSchemaBuilder.newSchemaBuilder("Student.proto");
        schemaBuilder.addMessageToProtoSchema(protobufMessage);
        Descriptor schema = schemaBuilder.build();

        DynamicMessage msg = generateDynamicMessage(person, schema, "Person");

        byte[] bytes = serialize(msg);

        try {
            DynamicMessage des = deserialize(schema, bytes);
            dynamicMessageToBMap(des);

        } catch (InvalidProtocolBufferException e) {

        }

    }

    public static ProtobufMessage generateSchema(BMap<BString, Object> bMap, String name) {
        ProtobufMessageBuilder nestedMessageBuilder = ProtobufMessage
                .newMessageBuilder(name.toUpperCase(Locale.getDefault()));
        int number = 1;

        for (Map.Entry<BString, Object> entry : bMap.entrySet()) {
            if (entry.getValue() instanceof BMap) {
                BMap<BString, Object> objToBMap = (BMap<BString, Object>) entry.getValue();
                ProtobufMessage nestedMessage = generateSchema(objToBMap, entry.getKey().toString());
                nestedMessageBuilder.addNestedMessage(nestedMessage);
                nestedMessageBuilder
                        .addField("required", entry.getKey().toString().toUpperCase(Locale.getDefault()),
                                entry.getKey().toString(), number);
                number++;

            } else {
                String dt = DataTypeMapper.getProtoFieldType(entry.getValue().getClass().getSimpleName());
                nestedMessageBuilder.addField("required", dt, entry.getKey().toString(), number);
                number++;
            }
        }
        return nestedMessageBuilder.build();
    }

    public static DynamicMessage generateDynamicMessage(BMap<BString, Object> bMap, Descriptor schema, String name) {

        DynamicMessage.Builder newMessageFromSchema = DynamicMessage.newBuilder(schema);
        Descriptor messageDescriptor = newMessageFromSchema.getDescriptorForType();

        for (Map.Entry<BString, Object> entry : bMap.entrySet()) {
            if (entry.getValue() instanceof BMap) {
                Descriptor subMessageDescriptor = schema
                        .findNestedTypeByName(entry.getKey().toString()
                                .toUpperCase(Locale.getDefault()));
                BMap<BString, Object> objToBMap = (BMap<BString, Object>) entry.getValue();
                DynamicMessage nestedMessage = generateDynamicMessage(objToBMap, subMessageDescriptor,
                        entry.getKey().toString());
                newMessageFromSchema.setField(messageDescriptor.findFieldByName(entry.getKey().toString()),
                        nestedMessage);
            } else {
                String fieldName = entry.getKey().toString();
                newMessageFromSchema.setField(messageDescriptor.findFieldByName(fieldName), entry.getValue());
            }
        }
        return newMessageFromSchema.build();
    }

    public static byte[] serialize(DynamicMessage message) {
        return message.toByteArray();
    }

    public static DynamicMessage deserialize(Descriptor schema, byte[] bytes) throws InvalidProtocolBufferException {
        return DynamicMessage.parseFrom(schema, bytes);
    }

    public static BMap<BString, Object> dynamicMessageToBMap(DynamicMessage message) {
        BMap<BString, Object> bMap = ValueCreator.createMapValue();
        for (Map.Entry<FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
            if (entry.getValue() instanceof DynamicMessage) {
                DynamicMessage msg = (DynamicMessage) entry.getValue();
                bMap.put(StringUtils.fromString(entry.getKey().getName()), dynamicMessageToBMap(msg));

            } else {
                if (entry.getValue().getClass().getSimpleName().equals("String")) {
                    bMap.put(StringUtils.fromString(entry.getKey().getName()),
                            StringUtils.fromString(entry.getValue().toString()));
                } else {
                    bMap.put(StringUtils.fromString(entry.getKey().getName()), entry.getValue());
                }
            }
        }

        return bMap;
    }

    public static void serdesFunction() throws DescriptorValidationException {
        ProtobufMessage nestedMessageBuilder = ProtobufMessage.newMessageBuilder("Phone")
                .addField("required", "string", "mobile", 1, "0773256")
                .addField("optional", "string", "home", 2, "4567892")
                .build();

        ProtobufSchemaBuilder schemaBuilder = ProtobufSchemaBuilder.newSchemaBuilder("Student.proto");
        ProtobufMessage messageBuilder = ProtobufMessage.newMessageBuilder("StudentMsg")
                .addField("required", "int32", "id", 1)
                .addField("required", "string", "name", 2)
                .addNestedMessage(nestedMessageBuilder)
                .addField("optional", "Phone", "phone", 3)
                .build();

        schemaBuilder.addMessageToProtoSchema(messageBuilder);
        Descriptor schema = schemaBuilder.build();

        DynamicMessage.Builder subMessageFromSchema = DynamicMessage.newBuilder(schema.findNestedTypeByName("Phone"));
        Descriptor subMessageDescriptor = subMessageFromSchema.getDescriptorForType();
        DynamicMessage subMessage = subMessageFromSchema
                .setField(subMessageDescriptor.findFieldByName("mobile"), "74848")
                .setField(subMessageDescriptor.findFieldByName("home"), "8745")
                .build();

        DynamicMessage.Builder newMessageFromSchema = DynamicMessage.newBuilder(schema);
        Descriptor messageDescriptor = newMessageFromSchema.getDescriptorForType();
        DynamicMessage message = newMessageFromSchema
                .setField(messageDescriptor.findFieldByName("id"), 1)
                .setField(messageDescriptor.findFieldByName("name"), "Tharinda Dilshan")
                .setField(messageDescriptor.findFieldByName("phone"), subMessage)
                .build();

        byte[] bytes = message.toByteArray();

        try {
            DynamicMessage.parseFrom(schema, bytes);
        } catch (InvalidProtocolBufferException e) {

        }
    }
}
