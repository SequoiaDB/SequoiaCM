package com.sequoiacm.infrastructure.feign;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bson.BSONObject;
import org.bson.util.JSON;

import java.io.IOException;
import java.net.URLDecoder;

public class BSONObjectJsonDeserializer<T extends BSONObject> extends StdDeserializer<T> {

    public BSONObjectJsonDeserializer() {
        super(BSONObject.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        String value = node.toString();
        String decodedValue = URLDecoder.decode(value, "UTF-8");
        T obj = (T) JSON.parse(decodedValue);
        return obj;
    }

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(BSONObject.class, new BSONObjectJsonDeserializer<>());
        mapper.registerModule(module);

        String json = "{\"a\":{\"$decimal\":\"12345678901234567890.123456789\"}}";
        BSONObject bson = mapper.readValue(json, BSONObject.class);
        System.out.println(bson.toString());
    }
}
