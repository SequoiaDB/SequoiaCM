package com.sequoiacm.contentserver.model.serial.gson;

import java.io.IOException;

import org.bson.BSONObject;
import org.bson.util.JSON;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class BSONObjectGsonTypeAdapter extends ScmGsonTypeAdapter<String, BSONObject> {

    private JsonParser jsonParser = new JsonParser();

    public BSONObjectGsonTypeAdapter() {
    }

    @Override
    public BSONObject convert(String source) {
        return (BSONObject) JSON.parse(source);
    }

    @Override
    public void write(JsonWriter out, BSONObject value) throws IOException {
        out.jsonValue(value.toString());
    }

    @Override
    public BSONObject read(JsonReader in) throws IOException {
        throw new IOException("do not supported read yet");
    }
}
