package com.sequoiacm.client.dispatcher;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.client.util.ScmHelper;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;
import org.bson.util.JSON;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;

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

    public RestBsonReader(InputStream is) throws ScmException {
        stream = is;
        Reader reader = null;
        try {
            reader = new InputStreamReader(stream, "utf-8");
            jsonReader = new JsonReader(reader);
            jsonReader.beginArray();
        }
        catch (IOException e) {
            ScmHelper.closeStream(stream);
            throw new ScmException(ScmError.NETWORK_IO, "failed to create json reader", e);
        }
        catch (Exception e) {
            ScmHelper.closeStream(stream);
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
    public BSONObject getNext() throws ScmException {
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
            throw new ScmException(ScmError.NETWORK_IO, "failed to get next element", e);
        }
    }

    @Override
    public void close() {
        ScmHelper.closeStream(stream);
        ScmHelper.closeStreamNoLogging(jsonReader);
    }
}
