package com.sequoiacm.contentserver.model.serial.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.ScmBucket;

import java.io.IOException;

public class BucketGsonTypeAdapter extends ScmGsonTypeAdapter<String, ScmBucket> {

    private Gson gson;

    public BucketGsonTypeAdapter() {
        GsonBuilder gb = new GsonBuilder();
        gb.registerTypeAdapter(BreakpointFile.class, this);
        gson = gb.create();
    }

    @Override
    public ScmBucket convert(String source) {
        return gson.fromJson(source, ScmBucket.class);
    }

    @Override
    public void write(JsonWriter out, ScmBucket value) throws IOException {

        out.beginObject();
        out.name(FieldName.Bucket.NAME).value(value.getName());
        out.name(FieldName.Bucket.ID).value(value.getId());
        out.name(FieldName.Bucket.CREATE_USER).value(value.getCreateUser());
        out.name(FieldName.Bucket.WORKSPACE).value(value.getWorkspace());
        out.name(FieldName.Bucket.CREATE_TIME).value(value.getCreateTime());
        out.endObject();
    }

    @Override
    public ScmBucket read(JsonReader in) throws IOException {
        throw new IOException("do not supported read yet");
    }
}
