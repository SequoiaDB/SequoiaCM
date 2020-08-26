package com.sequoiacm.content.client;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.bson.BSONObject;
import org.bson.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONReader;
import com.alibaba.fastjson.parser.Feature;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;

public abstract class ScmEleCursor<T> implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ScmEleCursor.class);

    private JSONReader jsonReader;
    private InputStream stream;

    public ScmEleCursor(InputStream is) throws ScmServerException {
        stream = is;
        Reader reader = null;
        try {
            reader = new InputStreamReader(stream, "utf-8");
            jsonReader = new JSONReader(reader, Feature.DisableCircularReferenceDetect);
            jsonReader.startArray();
        }
        catch (IOException e) {
            closeStream(stream);
            throw new ScmServerException(ScmError.NETWORK_IO, "failed to create json reder", e);
        }
        catch (Exception e) {
            closeStream(stream);
            throw new ScmServerException(ScmError.SYSTEM_ERROR, "failed to create json reder", e);
        }
    }

    public boolean hasNext() {
        return jsonReader.hasNext();
    }

    public T getNext() throws ScmServerException {
        if (jsonReader.hasNext()) {
            String str = jsonReader.readObject(String.class);
            BSONObject obj = (BSONObject) JSON.parse(str);
            return convert(obj);
        }
        else {
            return null;
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
