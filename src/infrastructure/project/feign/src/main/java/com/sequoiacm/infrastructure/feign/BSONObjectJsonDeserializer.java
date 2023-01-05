package com.sequoiacm.infrastructure.feign;

import java.io.IOException;

import org.bson.BSONObject;
import org.bson.util.JSON;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class BSONObjectJsonDeserializer<T extends BSONObject> extends StdDeserializer<T> {

    public BSONObjectJsonDeserializer() {
        super(BSONObject.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        String value = node.toString();
        T obj = (T) JSON.parse(value);
        return obj;
    }

}
