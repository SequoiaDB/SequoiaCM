package com.sequoiacm.infrastructrue.security.core;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ScmUserJsonSerializer extends StdSerializer<ScmUser> {

    /**
     *
     */
    private static final long serialVersionUID = -5339433912230424903L;

    private final ObjectMapper mapper;

    public ScmUserJsonSerializer() {
        super(ScmUser.class);
        SimpleModule module = new SimpleModule();
        module.addSerializer(ScmRole.class, new ScmRoleJsonSerializer());
        mapper = new ObjectMapper();
        mapper.registerModule(module);
    }

    @Override
    public void serialize(ScmUser value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField(ScmUser.JSON_FIELD_USER_ID, value.getUserId());
        gen.writeStringField(ScmUser.JSON_FIELD_USERNAME, value.getUsername());
        gen.writeStringField(ScmUser.JSON_FIELD_PASSWORD_TYPE, value.getPasswordType().name());
        gen.writeBooleanField(ScmUser.JSON_FIELD_ENABLED, value.isEnabled());
        gen.writeStringField(ScmUser.JSON_FIELD_ACCESS_KEY, value.getAccesskey());
        gen.writeFieldName(ScmUser.JSON_FIELD_ROLES);
        String tmp = mapper.writeValueAsString(value.getAuthorities());
        gen.writeRawValue(tmp);
        gen.writeEndObject();
    }

    // public static void main(String[] args) throws JsonProcessingException {
    // ScmUserJsonSerializer s = new ScmUserJsonSerializer();
    // ScmUser v = ScmUser.withUsername("name").userId("uid")
    // .passwordType(ScmUserPasswordType.TOKEN)
    // .roles(ScmRole.withRoleName("ROLE_rname").roleId("rid").build()).build();
    //
    // ObjectMapper mapper;
    // SimpleModule module = new SimpleModule();
    // module.addSerializer(ScmUser.class, new ScmUserGsonTypeAdapter());
    // module.addSerializer(ScmRole.class, new ScmRoleGsonTypeAdapter());
    // mapper = new ObjectMapper();
    // mapper.registerModule(module);
    //
    // System.out.println(mapper.writeValueAsString(v));
    // }
}
