package com.sequoiacm.infrastructrue.security.core;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ScmPrivilegeDeserilizer extends StdDeserializer<ScmPrivilege> {

    /**
     *
     */
    private static final long serialVersionUID = 3926006081105145288L;

    public ScmPrivilegeDeserilizer() {
        super(ScmPrivilege.class);
    }

    @Override
    public ScmPrivilege deserialize(JsonParser p, DeserializationContext c) throws IOException,
    JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        return deserialize(node);
    }

    public static ScmPrivilege deserialize(JsonNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Cannot parse null node");
        }

        if (!node.isObject()) {
            throw new IllegalArgumentException("Node should be object");
        }

        return new ScmPrivilege(JsonNodeUtils.textValue(node, ScmPrivilege.JSON_FIELD_ID),
                JsonNodeUtils.textValue(node, ScmPrivilege.JSON_FIELD_ROLE_TYPE),
                JsonNodeUtils.textValue(node, ScmPrivilege.JSON_FIELD_ROLE_ID),
                JsonNodeUtils.textValue(node, ScmPrivilege.JSON_FIELD_RESOURCE_ID),
                JsonNodeUtils.textValue(node, ScmPrivilege.JSON_FIELD_PRIVILEGE));
    }
}
