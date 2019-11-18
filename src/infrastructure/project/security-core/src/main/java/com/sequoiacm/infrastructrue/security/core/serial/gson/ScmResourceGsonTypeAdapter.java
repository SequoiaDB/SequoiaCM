package com.sequoiacm.infrastructrue.security.core.serial.gson;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.infrastructrue.security.core.ScmResource;

public class ScmResourceGsonTypeAdapter extends ScmGsonTypeAdapter<String, ScmResource> {

    public ScmResourceGsonTypeAdapter() {
    }

    @Override
    public ScmResource convert(String source) {
        throw new RuntimeException("do not support yet");
    }

    @Override
    public void write(JsonWriter out, ScmResource value) throws IOException {
        out.beginObject();
        out.name(ScmResource.JSON_FIELD_ID).value(value.getId());
        out.name(ScmResource.JSON_FIELD_TYPE).value(value.getType());
        out.name(ScmResource.JSON_FIELD_RESOURCE).value(value.getResource());
        out.endObject();
    }

    @Override
    public ScmResource read(JsonReader in) throws IOException {
        String id = null;
        String type = null;
        String resource = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case ScmResource.JSON_FIELD_ID:
                    id = in.nextString();
                    break;
                case ScmResource.JSON_FIELD_TYPE:
                    type = in.nextString();
                    break;
                case ScmResource.JSON_FIELD_RESOURCE:
                    resource = in.nextString();
                    break;
            }
        }
        in.endObject();

        return new ScmResource(id, type, resource);
    }
}
