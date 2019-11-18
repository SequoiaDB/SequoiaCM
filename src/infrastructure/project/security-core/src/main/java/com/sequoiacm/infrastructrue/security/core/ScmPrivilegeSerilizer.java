package com.sequoiacm.infrastructrue.security.core;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ScmPrivilegeSerilizer extends StdSerializer<ScmPrivilege> {
    /**
     *
     */
    private static final long serialVersionUID = 2999607788067731255L;

    public ScmPrivilegeSerilizer() {
        super(ScmPrivilege.class);
    }

    @Override
    public void serialize(ScmPrivilege value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField(ScmPrivilege.JSON_FIELD_ID, value.getId());
        gen.writeStringField(ScmPrivilege.JSON_FIELD_ROLE_TYPE, value.getRoleType());
        gen.writeStringField(ScmPrivilege.JSON_FIELD_ROLE_ID, value.getRoleId());
        gen.writeStringField(ScmPrivilege.JSON_FIELD_RESOURCE_ID, value.getResourceId());
        gen.writeStringField(ScmPrivilege.JSON_FIELD_PRIVILEGE, value.getPrivilege());
        gen.writeEndObject();
    }
}