package com.sequoiacm.infrastructrue.security.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ScmUserJsonDeserializer extends StdDeserializer<ScmUser> {

    /**
     *
     */
    private static final long serialVersionUID = 3516633398183674428L;

    private final ObjectMapper mapper;

    public ScmUserJsonDeserializer() {
        super(ScmUser.class);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ScmRole.class, new ScmRoleJsonDeserializer());
        mapper = new ObjectMapper();
        mapper.registerModule(module);
    }

    @Override
    public ScmUser deserialize(JsonParser p, DeserializationContext ctxt) throws IOException,
    JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        return deserialize(node);
    }

    public static ScmUser deserialize(BSONObject obj) {
        if(obj == null) {
            throw new IllegalArgumentException("Cannot pass null obj");
        }
        Collection<ScmRole> roles = new ArrayList<>();
        BasicBSONList rolesObj = (BasicBSONList) obj.get(ScmUser.JSON_FIELD_ROLES);
        if(rolesObj != null) {
            for(Object roleObj:rolesObj) {
                roles.add(ScmRoleJsonDeserializer.deserialize((BSONObject) roleObj));
            }
        }

        boolean hasPassword = true;
        String password = (String) obj.get(ScmUser.JSON_FIELD_PASSWORD);
        if (password == null || password.isEmpty()) {
            password = "No password";
            hasPassword = false;
        }

        String userName = (String) obj.get(ScmUser.JSON_FIELD_USERNAME);
        Assert.notNull(userName, "missing filed:obj=" + obj
                + ",field=" + ScmUser.JSON_FIELD_USERNAME);


        String userId = (String) obj.get(ScmUser.JSON_FIELD_USER_ID);
        Assert.notNull(userId, "missing filed:obj=" + obj
                + ",field=" + ScmUser.JSON_FIELD_USER_ID);

        String pwdType = (String) obj.get(ScmUser.JSON_FIELD_PASSWORD_TYPE);
        Assert.notNull(pwdType, "missing filed:obj=" + obj
                + ",field=" + ScmUser.JSON_FIELD_PASSWORD_TYPE);

        Boolean enable = (Boolean) obj.get(ScmUser.JSON_FIELD_ENABLED);
        Assert.notNull(enable, "missing filed:obj=" + obj
                + ",field=" + ScmUser.JSON_FIELD_ENABLED);

        ScmUser user = ScmUser.withUsername(userName).userId(userId)
                .passwordType(ScmUserPasswordType.valueOf(pwdType))
                .disabled(!enable.booleanValue()).roles(roles)
                .password(password).build();

        if (!hasPassword) {
            user.eraseCredentials();
        }

        return user;
    }

    public static ScmUser deserialize(JsonNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Cannot pass null node");
        }

        if (!node.isObject()) {
            throw new IllegalArgumentException("Node should be object");
        }

        Collection<ScmRole> roles = new ArrayList<>();
        JsonNode rolesNode = node.get(ScmUser.JSON_FIELD_ROLES);
        if (rolesNode != null && rolesNode.isArray()) {
            Iterator<JsonNode> nodes = rolesNode.elements();
            while (nodes.hasNext()) {
                JsonNode roleNode = nodes.next();
                ScmRole role = ScmRoleJsonDeserializer.deserialize(roleNode);
                roles.add(role);
            }
        }

        boolean hasPassword = true;
        String password = null;
        JsonNode passwordNode = node.get(ScmUser.JSON_FIELD_PASSWORD);
        if (passwordNode != null) {
            password = passwordNode.textValue();
        }
        if (password == null || password.isEmpty()) {
            password = "No password";
            hasPassword = false;
        }

        ScmUser user = ScmUser
                .withUsername(JsonNodeUtils.textValue(node, ScmUser.JSON_FIELD_USERNAME))
                .userId(JsonNodeUtils.textValue(node, ScmUser.JSON_FIELD_USER_ID))
                .passwordType(
                        ScmUserPasswordType.valueOf(JsonNodeUtils.textValue(node,
                                ScmUser.JSON_FIELD_PASSWORD_TYPE)))
                .disabled(!JsonNodeUtils.booleanValue(node, ScmUser.JSON_FIELD_ENABLED))
                .roles(roles).password(password).build();

        if (!hasPassword) {
            user.eraseCredentials();
        }

        return user;
    }
}
