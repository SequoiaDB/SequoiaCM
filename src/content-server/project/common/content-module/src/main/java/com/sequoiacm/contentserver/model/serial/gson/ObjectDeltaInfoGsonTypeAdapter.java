package com.sequoiacm.contentserver.model.serial.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.model.ObjectDeltaInfo;

import java.io.IOException;

public class ObjectDeltaInfoGsonTypeAdapter extends ScmGsonTypeAdapter<String, ObjectDeltaInfo> {
    private Gson gson;

    public ObjectDeltaInfoGsonTypeAdapter() {
        GsonBuilder gb = new GsonBuilder();
        gson = gb.create();
    }

    @Override
    public void write(JsonWriter jsonWriter, ObjectDeltaInfo objectDeltaInfo) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name(FieldName.ObjectDelta.FIELD_BUCKET_NAME)
                .value(objectDeltaInfo.getBucketName());
        jsonWriter.name(FieldName.ObjectDelta.FIELD_COUNT_DELTA).value(objectDeltaInfo.getCount());
        jsonWriter.name(FieldName.ObjectDelta.FIELD_SIZE_DELTA).value(objectDeltaInfo.getSumSize());
        jsonWriter.endObject();
    }

    @Override
    public ObjectDeltaInfo read(JsonReader jsonReader) throws IOException {
        throw new IOException("do not supported read yet");
    }

    @Override
    public ObjectDeltaInfo convert(String s) {
        return gson.fromJson(s, ObjectDeltaInfo.class);

    }
}
