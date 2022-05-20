package com.sequoiacm.content.client;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;

public abstract class ScmEleCursor<T> implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ScmEleCursor.class);

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

    public ScmEleCursor(InputStream is) throws ScmServerException {
        stream = is;
        Reader reader = null;
        try {
            reader = new InputStreamReader(stream, "utf-8");
            jsonReader = new JsonReader(reader);
            jsonReader.beginArray();
        }
        catch (IOException e) {
            closeStream(stream);
            throw new ScmServerException(ScmError.NETWORK_IO, "failed to create json reader", e);
        }
        catch (Exception e) {
            closeStream(stream);
            throw new ScmServerException(ScmError.SYSTEM_ERROR, "failed to create json reader", e);
        }
    }

    public boolean hasNext() {
        try {
            return jsonReader.hasNext();
        }
        catch (IOException e) {
            throw new JsonIOException("failed to reader next element", e);
        }
    }

    public T getNext() throws ScmServerException {
        try {
            if (jsonReader.hasNext()) {
                String str = gson.fromJson(jsonReader, String.class);
                BSONObject obj = (BSONObject) JSON.parse(str);
                return convert(obj);
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            throw new ScmServerException(ScmError.NETWORK_IO, "failed to get next element", e);
        }
    }

    protected abstract T convert(BSONObject b) throws ScmServerException;

    @Override
    public void close() {
        closeStream(jsonReader);
        closeStream(stream);
    }

    public void closeStream(Closeable stream) {
        try {
            if (null != stream) {
                stream.close();
            }
        }
        catch (Exception e) {
        }
    }
}
