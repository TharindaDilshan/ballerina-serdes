import java.util.HashMap;
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
        phone.put(StringUtils.fromString("mobile"), 123456);
        phone.put(StringUtils.fromString("home"), 6666666);

        // BMap<BString, Object> address = ValueCreator.createMapValue();
        // address.put(StringUtils.fromString("street1"), StringUtils.fromString("abcd"));
        // address.put(StringUtils.fromString("street2"), StringUtils.fromString("qwerty"));

        contact.put(StringUtils.fromString("phone"), phone);
        // contact.put(StringUtils.fromString("address"), address);
        person.put(StringUtils.fromString("contact"), contact);


        ProtobufMessage builds = generateSchema(person, "person");
        System.out.println(builds.getProtobufMessage());
    }

    public static Map<String, Object> builderList = new HashMap<>();
    public static int i = 1;

    public static ProtobufMessage generateSchema(BMap<BString, Object> bMap, String name){
        // Map<String, Object> builder = new HashMap<>();
        ProtobufMessageBuilder nestedMessageBuilder = ProtobufMessage.newMessageBuilder(name);
        int number = 1;

        for (Map.Entry<BString, Object> entry : bMap.entrySet()) {
                // System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue().getClass().getSimpleName()); 
                if(entry.getValue() instanceof BMap) {
                        BMap<BString, Object> objToBMap = (BMap<BString, Object>) entry.getValue();
                        ProtobufMessage nestedMessage = generateSchema(objToBMap, entry.getKey().toString());
                        nestedMessageBuilder.addNestedMessage(nestedMessage);
                        nestedMessageBuilder.addField("required", entry.getKey().toString(), entry.getKey().toString(), number);
                        number++;
                        // builder.put(entry.getKey().toString(), generateSchema(objToBMap, entry.getKey().toString()));

                }else {
                        String dt = DataTypeMapper.getDataType(entry.getValue().getClass().getSimpleName());
                        // builder.put(entry.getKey().toString(), dt);
                        nestedMessageBuilder.addField("required", dt, entry.getKey().toString(), number);
                        number++;
                }
        } 
        // builderList.put(String.valueOf(i), builder);
        // i++;
        return nestedMessageBuilder.build();
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
