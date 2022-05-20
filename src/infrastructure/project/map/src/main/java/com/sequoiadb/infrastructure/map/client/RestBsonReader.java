package com.sequoiadb.infrastructure.map.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import org.bson.BSONObject;
import org.bson.util.JSON;

import com.sequoiadb.infrastructure.map.CommonHelper;
import com.sequoiadb.infrastructure.map.ScmMapError;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.ScmSystemException;

public class RestBsonReader implements BsonReader {
    private JsonReader jsonReader;
    private InputStream stream;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(String.class, new JsonDeserializer<String>() {
                @Override
                public String deserialize(JsonElement json, Type typeOfT,
                        JsonDeserializationContext context) throws JsonParseException {
                    return json.toString();
                }
            }).create();

    public RestBsonReader(InputStream is) throws ScmMapServerException {
        stream = is;
        Reader reader = null;
        try {
            reader = new InputStreamReader(stream, "utf-8");
            jsonReader = new JsonReader(reader);
            jsonReader.beginArray();
        }
        catch (IOException e) {
            CommonHelper.close(stream);
            throw new ScmMapServerException(ScmMapError.NETWORK_IO, "failed to create json reader",
                    e);
        }
        catch (Exception e) {
            CommonHelper.close(stream);
            throw new ScmSystemException("failed to create json reader", e);
        }
    }

    @Override
    public boolean hasNext() {
        try {
            return jsonReader.hasNext();
        }
        catch (IOException e) {
            throw new JsonIOException("failed to reader next element", e);
        }
    }

    @Override
    public BSONObject getNext() throws ScmMapServerException {
        try {
            if (jsonReader.hasNext()) {
                String str = gson.fromJson(jsonReader, String.class);
                return (BSONObject) JSON.parse(str);
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            throw new ScmMapServerException(ScmMapError.NETWORK_IO, "failed to get next element",
                    e);
        }
    }

    @Override
    public void close() {
        CommonHelper.close(stream);
        CommonHelper.closeNoLogging(jsonReader);
    }
}
