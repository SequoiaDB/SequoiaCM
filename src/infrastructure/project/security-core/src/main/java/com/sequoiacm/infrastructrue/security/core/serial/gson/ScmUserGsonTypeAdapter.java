package com.sequoiacm.infrastructrue.security.core.serial.gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.infrastructrue.security.core.ScmRole;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserPasswordType;

public class ScmUserGsonTypeAdapter extends ScmGsonTypeAdapter<String, ScmUser> {

    private ScmRoleGsonTypeAdapter roleTypeAdpater = new ScmRoleGsonTypeAdapter();

    public ScmUserGsonTypeAdapter() {
    }

    @Override
    public ScmUser convert(String source) {
        throw new RuntimeException("do not support yet");
    }

    @Override
    public void write(JsonWriter out, ScmUser value) throws IOException {
        out.beginObject();
        out.name(ScmUser.JSON_FIELD_USER_ID).value(value.getUserId());
        out.name(ScmUser.JSON_FIELD_USERNAME).value(value.getUsername());
        out.name(ScmUser.JSON_FIELD_PASSWORD_TYPE).value(value.getPasswordType().name());
        out.name(ScmUser.JSON_FIELD_ENABLED).value(value.isEnabled());
        out.name(ScmUser.JSON_FIELD_ACCESS_KEY).value(value.getAccesskey());
        writeArray(out, ScmUser.JSON_FIELD_ROLES, value.getAuthorities());
        out.endObject();

    }

    private void writeArray(JsonWriter out, String name, Collection<ScmRole> roles)
            throws IOException {
        out.name(name);
        out.beginArray();

        if (null != roles) {
            for (ScmRole r : roles) {
                roleTypeAdpater.write(out, r);
            }
        }

        out.endArray();
    }

    @Override
    public ScmUser read(JsonReader in) throws IOException {
        String userId = null;
        String userName = null;
        String passwdType = null;
        boolean hasPasswd = false;
        String passwd = "no passwd";
        boolean enabled = false;
        List<ScmRole> roleList = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case ScmUser.JSON_FIELD_USER_ID:
                    userId = in.nextString();
                    break;
                case ScmUser.JSON_FIELD_USERNAME:
                    userName = in.nextString();
                    break;
                case ScmUser.JSON_FIELD_PASSWORD_TYPE:
                    passwdType = in.nextString();
                    break;
                case ScmUser.JSON_FIELD_PASSWORD:
                    passwd = in.nextString();
                    hasPasswd = true;
                    break;
                case ScmUser.JSON_FIELD_ENABLED:
                    enabled = in.nextBoolean();
                    break;
                case ScmUser.JSON_FIELD_ROLES:
                    roleList = readRoles(in);
                    break;
            }
        }
        in.endObject();

        ScmUser user = ScmUser.withUsername(userName).userId(userId).disabled(!enabled)
                .roles(roleList).passwordType(ScmUserPasswordType.valueOf(passwdType))
                .password(passwd).build();

        if (!hasPasswd) {
            user.eraseCredentials();
        }

        return user;
    }

    private List<ScmRole> readRoles(JsonReader in) throws IOException {
        List<ScmRole> roleList = new ArrayList<>();

        in.beginArray();
        while (in.hasNext()) {
            roleList.add(roleTypeAdpater.read(in));
        }
        in.endArray();

        return roleList;
    }
}
