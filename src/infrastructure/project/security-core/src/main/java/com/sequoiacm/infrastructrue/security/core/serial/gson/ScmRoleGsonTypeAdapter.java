package com.sequoiacm.infrastructrue.security.core.serial.gson;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.infrastructrue.security.core.ScmRole;

public class ScmRoleGsonTypeAdapter extends ScmGsonTypeAdapter<String, ScmRole> {

    public ScmRoleGsonTypeAdapter() {
    }

    @Override
    public ScmRole convert(String source) {
        throw new RuntimeException("do not support yet");
    }

    @Override
    public void write(JsonWriter out, ScmRole value) throws IOException {
        out.beginObject();
        out.name(ScmRole.JSON_FIELD_ROLE_ID).value(value.getRoleId());
        out.name(ScmRole.JSON_FIELD_ROLE_NAME).value(value.getRoleName());
        out.name(ScmRole.JSON_FIELD_DESCRIPTION).value(value.getDescription());
        out.endObject();

    }

    @Override
    public ScmRole read(JsonReader in) throws IOException {
        String roleName = null;
        String roleId = null;
        String desc = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case ScmRole.JSON_FIELD_ROLE_NAME:
                    roleName = in.nextString();
                    break;
                case ScmRole.JSON_FIELD_ROLE_ID:
                    roleId = in.nextString();
                    break;
                case ScmRole.JSON_FIELD_DESCRIPTION:
                    desc = in.nextString();
                    break;
            }
        }

        in.endObject();

        return ScmRole.withRoleName(roleName).roleId(roleId).description(desc).build();
    }

}
