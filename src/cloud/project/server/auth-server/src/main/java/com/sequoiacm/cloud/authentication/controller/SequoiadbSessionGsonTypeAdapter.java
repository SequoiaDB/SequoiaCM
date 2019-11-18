package com.sequoiacm.cloud.authentication.controller;

import java.io.IOException;

import org.springframework.session.data.sequoiadb.SequoiadbSession;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.serial.gson.ScmUserGsonTypeAdapter;

public class SequoiadbSessionGsonTypeAdapter extends TypeAdapter<SequoiadbSession> {

    private ScmUserGsonTypeAdapter typeAdapter = new ScmUserGsonTypeAdapter();

    @Override
    public void write(JsonWriter out, SequoiadbSession value) throws IOException {
        out.beginObject();
        out.name("session_id").value(value.getId());
        out.name("username").value(value.getPrincipal());
        out.name("creation_time").value(value.getCreationTime());
        out.name("last_accessed_time").value(value.getLastAccessedTime());
        out.name("max_inactive_interval").value(value.getMaxInactiveIntervalInSeconds());
        ScmUser user = value.getAttribute("user_details");
        if (user != null) {
            out.name("user_details");
            typeAdapter.write(out, user);
        }
        out.endObject();
    }

    @Override
    public SequoiadbSession read(JsonReader in) throws IOException {
        throw new RuntimeException("do not support yet");
    }
}
