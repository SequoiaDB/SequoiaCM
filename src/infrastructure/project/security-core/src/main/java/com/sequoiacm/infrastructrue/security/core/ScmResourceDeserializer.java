package com.sequoiacm.infrastructrue.security.core;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ScmResourceDeserializer extends StdDeserializer<ScmResource> {

    /**
     *
     */
    private static final long serialVersionUID = -6089469218364206332L;

    public ScmResourceDeserializer() {
        super(ScmResource.class);
    }

    @Override
    public ScmResource deserialize(JsonParser p, DeserializationContext c) throws IOException,
            JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        return deserialize(node);
    }

    public static ScmResource deserialize(JsonNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Cannot parse null node");
        }

        if (!node.isObject()) {
            throw new IllegalArgumentException("Node should be object");
        }

        return new ScmResource(JsonNodeUtils.textValue(node, ScmResource.JSON_FIELD_ID),
                JsonNodeUtils.textValue(node, ScmResource.JSON_FIELD_TYPE),
                JsonNodeUtils.textValue(node, ScmResource.JSON_FIELD_RESOURCE));
    }
}
