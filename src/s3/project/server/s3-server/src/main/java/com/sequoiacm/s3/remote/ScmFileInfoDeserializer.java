package com.sequoiacm.s3.remote;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class ScmFileInfoDeserializer extends JsonDeserializer<ScmFileInfo> {

    @Override
    public ScmFileInfo deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        ScmFileInfo info = new ScmFileInfo(node);
        return info;
    }
}
