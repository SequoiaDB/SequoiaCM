package com.sequoiacm.contentserver.model.serial.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Map;

public class MapGsonTypeAdapter extends ScmGsonTypeAdapter<String, Map<String, String>> {

    private Gson gson;

    public MapGsonTypeAdapter() {
        GsonBuilder gb = new GsonBuilder();
        gb.serializeNulls();
        gson = gb.create();
    }

    @Override
    public Map<String, String> convert(String source) {
        return gson.fromJson(source, Map.class);
    }

    @Override
    public void write(JsonWriter out, Map<String, String> value) throws IOException {
        out.jsonValue(gson.toJson(value));
    }

    @Override
    public Map<String, String> read(JsonReader in) throws IOException {
        return gson.fromJson(in, Map.class);
    }
}
