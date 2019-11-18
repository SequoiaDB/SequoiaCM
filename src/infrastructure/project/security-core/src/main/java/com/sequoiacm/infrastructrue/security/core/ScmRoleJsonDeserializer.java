package com.sequoiacm.infrastructrue.security.core;

import java.io.IOException;

import org.bson.BSONObject;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.NullNode;

public class ScmRoleJsonDeserializer extends StdDeserializer<ScmRole> {

    /**
     *
     */
    private static final long serialVersionUID = 5225027589446372351L;

    public ScmRoleJsonDeserializer() {
        super(ScmRole.class);
    }

    @Override
    public ScmRole deserialize(JsonParser p, DeserializationContext ctxt) throws IOException,
    JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        return deserialize(node);
    }

    public static ScmRole deserialize(BSONObject obj) {
        if(obj == null) {
            throw new IllegalArgumentException("Cannot pass null obj");
        }

        String roleName = (String) obj.get(ScmRole.JSON_FIELD_ROLE_NAME);
        Assert.notNull(roleName,"missing filed:obj=" + obj
                + ",field=" + ScmRole.JSON_FIELD_ROLE_NAME);

        String roleId = (String) obj.get(ScmRole.JSON_FIELD_ROLE_ID);
        Assert.notNull(roleId,"missing filed:obj=" + obj
                + ",field=" + ScmRole.JSON_FIELD_ROLE_ID);

        String desc = (String) obj.get(ScmRole.JSON_FIELD_DESCRIPTION);
        return ScmRole.withRoleName(roleName).roleId(roleId).description(desc).build();
    }

    public static ScmRole deserialize(JsonNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Cannot pass null node");
        }

        if (!node.isObject()) {
            throw new IllegalArgumentException("Node should be object");
        }

        return ScmRole
                .withRoleName(JsonNodeUtils.textValue(node, ScmRole.JSON_FIELD_ROLE_NAME))
                .roleId(JsonNodeUtils.textValue(node, ScmRole.JSON_FIELD_ROLE_ID))
                .description(
                        JsonNodeUtils.getOrNull(node, ScmRole.JSON_FIELD_DESCRIPTION,
                                NullNode.getInstance()).textValue()).build();
    }
}
