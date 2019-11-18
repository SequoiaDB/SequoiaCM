package com.sequoiacm.infrastructrue.security.core;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ScmRoleJsonSerializer extends StdSerializer<ScmRole> {

    /**
     *
     */
    private static final long serialVersionUID = 2712796124670872334L;

    public ScmRoleJsonSerializer() {
        super(ScmRole.class);
    }

    @Override
    public void serialize(ScmRole value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField(ScmRole.JSON_FIELD_ROLE_ID, value.getRoleId());
        gen.writeStringField(ScmRole.JSON_FIELD_ROLE_NAME, value.getRoleName());
        gen.writeStringField(ScmRole.JSON_FIELD_DESCRIPTION, value.getDescription());
        gen.writeEndObject();
    }
}
