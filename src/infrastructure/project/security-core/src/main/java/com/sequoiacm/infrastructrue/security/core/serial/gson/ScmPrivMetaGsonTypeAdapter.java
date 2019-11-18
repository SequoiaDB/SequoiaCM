package com.sequoiacm.infrastructrue.security.core.serial.gson;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.infrastructrue.security.core.ScmPrivMeta;

public class ScmPrivMetaGsonTypeAdapter extends ScmGsonTypeAdapter<String, ScmPrivMeta> {

    public ScmPrivMetaGsonTypeAdapter() {
    }

    @Override
    public ScmPrivMeta convert(String source) {
        throw new RuntimeException("do not support yet");
    }

    @Override
    public void write(JsonWriter out, ScmPrivMeta value) throws IOException {
        out.beginObject();
        out.name(ScmPrivMeta.JSON_FIELD_VERSION).value(value.getVersion());
        out.endObject();

    }

    @Override
    public ScmPrivMeta read(JsonReader in) throws IOException {
        long version = 0;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case ScmPrivMeta.JSON_FIELD_VERSION:
                    version = in.nextLong();
                    break;
            }
        }
        in.endObject();

        return new ScmPrivMeta((int) version);
    }
}
