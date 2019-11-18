package com.sequoiacm.infrastructrue.security.core;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ScmPrivMetaSerializer extends StdSerializer<ScmPrivMeta> {
    /**
     *
     */
    private static final long serialVersionUID = 2999607788067731255L;

    public ScmPrivMetaSerializer() {
        super(ScmPrivMeta.class);
    }

    @Override
    public void serialize(ScmPrivMeta value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.writeNumberField(ScmPrivMeta.JSON_FIELD_VERSION, value.getVersion());
        gen.writeEndObject();
    }
}