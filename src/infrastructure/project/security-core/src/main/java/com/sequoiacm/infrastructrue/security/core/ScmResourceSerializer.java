package com.sequoiacm.infrastructrue.security.core;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ScmResourceSerializer extends StdSerializer<ScmResource> {

    /**
     *
     */
    private static final long serialVersionUID = 1006851544109353632L;

    public ScmResourceSerializer() {
        super(ScmResource.class);
    }

    @Override
    public void serialize(ScmResource value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField(ScmResource.JSON_FIELD_ID, value.getId());
        gen.writeStringField(ScmResource.JSON_FIELD_TYPE, value.getType());
        gen.writeStringField(ScmResource.JSON_FIELD_RESOURCE, value.getResource());
        gen.writeEndObject();
    }
}