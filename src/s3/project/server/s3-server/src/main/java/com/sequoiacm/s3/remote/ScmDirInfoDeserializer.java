package com.sequoiacm.s3.remote;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequoiacm.common.FieldName;

public class ScmDirInfoDeserializer extends JsonDeserializer<ScmDirInfo> {

    @Override
    public ScmDirInfo deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        ScmDirInfo info = new ScmDirInfo();
        info.setId(node.get(FieldName.FIELD_CLDIR_ID).asText());
        info.setName(node.get(FieldName.FIELD_CLDIR_NAME).asText());
        info.setParentId(node.get(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID).asText());
        info.setCreateTime(node.get(FieldName.FIELD_CLDIR_CREATE_TIME).asLong());
        info.setUser(node.get(FieldName.FIELD_CLDIR_USER).asText());
        return info;
    }

}
