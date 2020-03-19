package com.sequoiacm.s3.remote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;

public class ScmWsInfoDeserializer extends JsonDeserializer<ScmWsInfo> {

    @Override
    public ScmWsInfo deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        ScmWsInfo info = new ScmWsInfo();
        if (node.size() == 1 && node.has(CommonDefine.RestArg.GET_WORKSPACE_REPS)) {
            node = node.get(CommonDefine.RestArg.GET_WORKSPACE_REPS);
        }
        info.setId(node.get(FieldName.FIELD_CLWORKSPACE_ID).asInt());
        info.setName(node.get(FieldName.FIELD_CLWORKSPACE_NAME).asText());
        info.setCreateTime(node.get(FieldName.FIELD_CLWORKSPACE_CREATETIME).asLong());
        List<Integer> sites = new ArrayList<>();
        for (JsonNode e : node.get(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION)) {
            sites.add(e.get(FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID).asInt());
        }
        info.setSites(sites);
        return info;
    }

}
