package com.sequoiacm.infrastructrue.security.core.serial.gson;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.infrastructrue.security.core.ScmPrivilege;

public class ScmPrivilegeGsonTypeAdapter extends ScmGsonTypeAdapter<String, ScmPrivilege> {

    private Gson gson;

    public ScmPrivilegeGsonTypeAdapter() {
        GsonBuilder gb = new GsonBuilder();
        gb.registerTypeAdapter(ScmPrivilege.class, this);
        gson = gb.create();
    }

    @Override
    public ScmPrivilege convert(String source) {
        return gson.fromJson(source, ScmPrivilege.class);
    }

    @Override
    public void write(JsonWriter out, ScmPrivilege value) throws IOException {
        out.beginObject();
        out.name(ScmPrivilege.JSON_FIELD_ID).value(value.getId());
        out.name(ScmPrivilege.JSON_FIELD_ROLE_TYPE).value(value.getRoleType());
        out.name(ScmPrivilege.JSON_FIELD_ROLE_ID).value(value.getRoleId());
        out.name(ScmPrivilege.JSON_FIELD_RESOURCE_ID).value(value.getResourceId());
        out.name(ScmPrivilege.JSON_FIELD_PRIVILEGE).value(value.getPrivilege());
        out.endObject();
    }

    @Override
    public ScmPrivilege read(JsonReader in) throws IOException {
        String id = null;
        String roleType = null;
        String roleId = null;
        String resourceId = null;
        String privilege = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case ScmPrivilege.JSON_FIELD_ID:
                    id = in.nextString();
                    break;
                case ScmPrivilege.JSON_FIELD_ROLE_TYPE:
                    roleType = in.nextString();
                    break;
                case ScmPrivilege.JSON_FIELD_ROLE_ID:
                    roleId = in.nextString();
                    break;
                case ScmPrivilege.JSON_FIELD_RESOURCE_ID:
                    resourceId = in.nextString();
                    break;
                case ScmPrivilege.JSON_FIELD_PRIVILEGE:
                    privilege = in.nextString();
                    break;
            }
        }
        in.endObject();

        return new ScmPrivilege(id, roleType, roleId, resourceId, privilege);
    }
}
