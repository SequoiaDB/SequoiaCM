package com.sequoiacm.infrastructrue.security.core.serial.gson;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.infrastructrue.security.core.AccesskeyInfo;

public class AccesskeyInfoGsonTypeAdapter extends ScmGsonTypeAdapter<String, AccesskeyInfo> {

    public AccesskeyInfoGsonTypeAdapter() {
    }

    @Override
    public AccesskeyInfo convert(String source) {
        throw new RuntimeException("do not support yet");
    }

    @Override
    public void write(JsonWriter out, AccesskeyInfo value) throws IOException {
        out.beginObject();
        out.name(AccesskeyInfo.JSON_FIELD_ACCESSKEY).value(value.getAccesskey());
        out.name(AccesskeyInfo.JSON_FIELD_SECRETKEY).value(value.getSecretkey());
        out.name(AccesskeyInfo.JSON_FIELD_USERNAME).value(value.getUsername());
        out.endObject();

    }

    @Override
    public AccesskeyInfo read(JsonReader in) throws IOException {
        throw new RuntimeException("do not support yet");
    }

}
