package com.sequoiacm.contentserver.model.serial.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sequoiacm.contentserver.model.DataTableDeleteOption;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.util.JSON;

import java.io.IOException;
import java.lang.reflect.Type;

public class DataTableDeleteOptionGsonTypeAdapter
        extends ScmGsonTypeAdapter<String, DataTableDeleteOption> {
    private Gson gson;

    public DataTableDeleteOptionGsonTypeAdapter() {
        GsonBuilder gb = new GsonBuilder();
        gson = gb.registerTypeAdapter(DataTableDeleteOption.class, this)
                .registerTypeAdapter(String.class, new JsonDeserializer<String>() {
                    @Override
                    public String deserialize(JsonElement json, Type typeOfT,
                            JsonDeserializationContext context) throws JsonParseException {
                        return json.toString();
                    }
                }).serializeNulls().create();
    }

    @Override
    public void write(JsonWriter out, DataTableDeleteOption value) throws IOException {
        out.beginObject();
        if (value.getWsLocalSiteLocation() != null) {
            out.name("wsLocalSiteLocation").jsonValue(value.getWsLocalSiteLocation().toString());
        }
        out.endObject();
    }

    @Override
    public DataTableDeleteOption read(JsonReader in) throws IOException {
        BSONObject wsLocalSiteLocation;
        BSONObject bsonObject;
        String s = gson.fromJson(in, String.class);
        bsonObject = (BSONObject) JSON.parse(s);
        wsLocalSiteLocation = BsonUtils.getBSON(bsonObject, "wsLocalSiteLocation");
        return new DataTableDeleteOption(wsLocalSiteLocation);
    }

    @Override
    public DataTableDeleteOption convert(String source) {
        BSONObject wsLocalSiteLocation;
        BSONObject bsonObject;
        bsonObject = (BSONObject) JSON.parse(source);
        wsLocalSiteLocation = BsonUtils.getBSON(bsonObject, "wsLocalSiteLocation");
        return new DataTableDeleteOption(wsLocalSiteLocation);
    }
}
