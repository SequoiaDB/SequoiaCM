package com.sequoiacm.client.dispatcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.bson.BSONObject;
import org.bson.util.JSON;

import com.alibaba.fastjson.JSONReader;
import com.alibaba.fastjson.parser.Feature;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.client.util.ScmHelper;
import com.sequoiacm.exception.ScmError;

public class RestBsonReader implements BsonReader {
    private JSONReader jsonReader;
    private InputStream stream;

    public RestBsonReader(InputStream is) throws ScmException {
        stream = is;
        Reader reader = null;
        try {
            reader = new InputStreamReader(stream, "utf-8");
            jsonReader = new JSONReader(reader, Feature.DisableCircularReferenceDetect);
            jsonReader.startArray();
        }
        catch (IOException e) {
            ScmHelper.closeStream(stream);
            throw new ScmException(ScmError.NETWORK_IO, "failed to create json reder", e);
        }
        catch (Exception e) {
            ScmHelper.closeStream(stream);
            throw new ScmSystemException(
                    "failed to create json reder", e);
        }
    }

    @Override
    public boolean hasNext() {
        return jsonReader.hasNext();
    }

    @Override
    public BSONObject getNext() {
        if (jsonReader.hasNext()) {
            String str = jsonReader.readObject(String.class);
            BSONObject obj = (BSONObject) JSON.parse(str);
            return obj;
        }
        else {
            return null;
        }
    }

    @Override
    public void close() {
        ScmHelper.closeStream(stream);
        ScmHelper.closeStreamNoLogging(jsonReader);
    }
}
