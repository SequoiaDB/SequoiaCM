package com.sequoiacm.s3.remote;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequoiacm.infrastructrue.security.core.AccesskeyInfo;

public class AccesskeyInfoDeserializer extends JsonDeserializer<AccesskeyInfo> {

    @Override
    public AccesskeyInfo deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        String accesskey = node.get(AccesskeyInfo.JSON_FIELD_ACCESSKEY).asText();
        String username = node.get(AccesskeyInfo.JSON_FIELD_USERNAME).asText();
        String secretkey = node.get(AccesskeyInfo.JSON_FIELD_SECRETKEY).asText();
        return new AccesskeyInfo(accesskey, secretkey, username);
    }

}
