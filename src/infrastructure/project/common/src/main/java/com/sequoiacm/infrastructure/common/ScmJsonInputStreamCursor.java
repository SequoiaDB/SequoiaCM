package com.sequoiacm.infrastructure.common;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ScmJsonInputStreamCursor<T> implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ScmJsonInputStreamCursor.class);

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(String.class, new JsonDeserializer<String>() {
                @Override
                public String deserialize(JsonElement json, Type typeOfT,
                        JsonDeserializationContext context) throws JsonParseException {
                    return json.toString();
                }
            }).create();

    private JsonReader jsonReader;
    private InputStream stream;

    public ScmJsonInputStreamCursor(InputStream is) throws IOException {
        stream = is;
        Reader reader = null;
        try {
            reader = new InputStreamReader(stream, "utf-8");
            jsonReader = new JsonReader(reader);
            jsonReader.beginArray();
        }
        catch (IOException e) {
            closeStream(stream);
            throw e;
        }
        catch (Exception e) {
            closeStream(stream);
            throw new IOException("failed to create json reader", e);
        }

    }

    public boolean hasNext() throws IOException {
        return jsonReader.hasNext();
    }

    public T getNext() throws IOException {
        if (jsonReader.hasNext()) {
            String str = gson.fromJson(jsonReader, String.class);
            BSONObject obj = (BSONObject) JSON.parse(str);
            return convert(obj);
        }
        else {
            return null;
        }
    }

    protected abstract T convert(BSONObject b);

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
            logger.warn("failed to close stream", e);
        }
    }
}
