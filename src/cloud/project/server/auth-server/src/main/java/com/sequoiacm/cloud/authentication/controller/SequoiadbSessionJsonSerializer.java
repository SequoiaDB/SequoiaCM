package com.sequoiacm.cloud.authentication.controller;

import java.io.IOException;

import org.springframework.session.data.sequoiadb.SequoiadbSession;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmRoleJsonSerializer;
import com.sequoiacm.infrastructrue.security.core.ScmUser;

public class SequoiadbSessionJsonSerializer extends StdSerializer<SequoiadbSession> {
    private static final long serialVersionUID = 8166528461669793957L;
    private final ObjectMapper mapper;

    public SequoiadbSessionJsonSerializer() {
        super(SequoiadbSession.class);
        SimpleModule module = new SimpleModule();
        module.addSerializer(ScmRole.class, new ScmRoleJsonSerializer());
        mapper = new ObjectMapper();
        mapper.registerModule(module);
    }

    @Override
    public void serialize(SequoiadbSession value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField("session_id", value.getId());
        gen.writeStringField("username", value.getPrincipal());
        gen.writeNumberField("creation_time", value.getCreationTime());
        gen.writeNumberField("last_accessed_time", value.getLastAccessedTime());
        gen.writeNumberField("max_inactive_interval", value.getMaxInactiveIntervalInSeconds());
        ScmUser user = value.getAttribute("user_details");
        if (user != null) {
            gen.writeFieldName("user_details");
            String tmp = mapper.writeValueAsString(user);
            gen.writeRaw(tmp);
        }
        gen.writeEndObject();
    }
}
