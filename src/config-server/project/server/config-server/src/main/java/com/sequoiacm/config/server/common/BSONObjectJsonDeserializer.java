package com.sequoiacm.config.server.common;

import java.io.IOException;
import java.net.URLDecoder;

import org.bson.BSONObject;
import org.bson.util.JSON;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class BSONObjectJsonDeserializer extends StdDeserializer<BSONObject> {

    public BSONObjectJsonDeserializer() {
        super(BSONObject.class);
    }

    @Override
    public BSONObject deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        String value = node.toString();
        String decodedValue = URLDecoder.decode(value, "UTF-8");
        BSONObject obj = (BSONObject) JSON.parse(decodedValue);
        return obj;
    }
}
