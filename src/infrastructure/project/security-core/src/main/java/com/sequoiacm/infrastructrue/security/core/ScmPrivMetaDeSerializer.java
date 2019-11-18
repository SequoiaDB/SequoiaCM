package com.sequoiacm.infrastructrue.security.core;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ScmPrivMetaDeSerializer extends StdDeserializer<ScmPrivMeta> {

    /**
     *
     */
    private static final long serialVersionUID = -3326015785807247427L;

    public ScmPrivMetaDeSerializer() {
        super(ScmPrivMeta.class);
    }

    @Override
    public ScmPrivMeta deserialize(JsonParser p, DeserializationContext c) throws IOException,
            JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        return deserialize(node);
    }

    public static ScmPrivMeta deserialize(JsonNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Cannot parse null node");
        }

        if (!node.isObject()) {
            throw new IllegalArgumentException("Node should be object");
        }

        return new ScmPrivMeta(JsonNodeUtils.intValue(node, ScmPrivMeta.JSON_FIELD_VERSION));
    }
}