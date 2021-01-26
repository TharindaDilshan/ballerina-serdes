import java.util.Map;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
* SerDes class to create Dynamic messages.
*
* @author Tharinda.
* @return None.
* @throws DescriptorValidationException.
* @exception DescriptorValidationException.
*/
public class SerDes {
    public static void main(String[] args) throws Descriptors.DescriptorValidationException {
        // serdesFunction();
        BMap<BString, Object> person = ValueCreator.createMapValue();
        person.put(StringUtils.fromString("name"), StringUtils.fromString("Tharinda"));

        BMap<BString, Object> contact = ValueCreator.createMapValue();

        BMap<BString, Object> phone = ValueCreator.createMapValue();
        phone.put(StringUtils.fromString("mobile"), StringUtils.fromString("123456"));
        phone.put(StringUtils.fromString("home"), StringUtils.fromString("6666666"));

        BMap<BString, Object> address = ValueCreator.createMapValue();
        address.put(StringUtils.fromString("street1"), StringUtils.fromString("abcd"));
        address.put(StringUtils.fromString("street2"), StringUtils.fromString("qwerty"));

        contact.put(StringUtils.fromString("phone"), phone);
        contact.put(StringUtils.fromString("address"), address);
        person.put(StringUtils.fromString("contact"), contact);

        // System.out.println(person);

        for (Map.Entry<BString, Object> entry : person.entrySet()){
                System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue()); 
        }
    }

    public static void serdesFunction() throws Descriptors.DescriptorValidationException {
        ProtobufMessage nestedMessageBuilder = ProtobufMessage.newMessageBuilder("Phone")
                .addField("required", "string", "mobile", 1, "0773256")
                .addField("optional", "string", "home", 2, "4567892")
                .build();

        ProtobufSchemaBuilder schemaBuilder = ProtobufSchemaBuilder.newSchemaBuilder("Student.proto");
        ProtobufMessage messageBuilder = ProtobufMessage.newMessageBuilder("StudentMsg") // message Student
                .addField("required", "int32", "id", 1)        // required int32 id = 1
                .addField("required", "string", "name", 2)    // required string name = 2
                .addNestedMessage(nestedMessageBuilder)
                .addField("optional", "Phone", "phone", 3)
                .build();

        schemaBuilder.addMessageToProtoSchema(messageBuilder);
        Descriptors.Descriptor schema = schemaBuilder.build();

        Descriptors.Descriptor subMessageDescriptor = schema.findNestedTypeByName("Phone");
        DynamicMessage subMessage = DynamicMessage.newBuilder(schema.findNestedTypeByName("Phone"))
                .setField(subMessageDescriptor.findFieldByName("mobile"), "74848")
                .setField(subMessageDescriptor.findFieldByName("home"), "8745")
                .build();

        DynamicMessage.Builder newMessageFromSchema = DynamicMessage.newBuilder(schema);
        Descriptors.Descriptor messageDescriptor = newMessageFromSchema.getDescriptorForType();
        DynamicMessage message = newMessageFromSchema
                .setField(messageDescriptor.findFieldByName("id"), 1)
                .setField(messageDescriptor.findFieldByName("name"), "Tharinda Dilshan")
                .setField(messageDescriptor.findFieldByName("phone"), subMessage)
                .build();

        byte[] bytes = message.toByteArray();

        try {
        DynamicMessage des = DynamicMessage.parseFrom(schema, bytes);
        System.out.println(des);
        } catch (InvalidProtocolBufferException e) {

        }
    }
}
