package com.sequoiadb.infrastructure.map.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.bson.BSONObject;
import org.bson.util.JSON;

import com.alibaba.fastjson.JSONReader;
import com.alibaba.fastjson.parser.Feature;
import com.sequoiadb.infrastructure.map.CommonHelper;
import com.sequoiadb.infrastructure.map.ScmMapError;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.ScmSystemException;

public class RestBsonReader implements BsonReader {
    private JSONReader jsonReader;
    private InputStream stream;

    public RestBsonReader(InputStream is) throws ScmMapServerException {
        stream = is;
        Reader reader = null;
        try {
            reader = new InputStreamReader(stream, "utf-8");
            jsonReader = new JSONReader(reader, Feature.DisableCircularReferenceDetect);
            jsonReader.startArray();
        }
        catch (IOException e) {
            CommonHelper.close(stream);
            throw new ScmMapServerException(ScmMapError.NETWORK_IO, "failed to create json reder",
                    e);
        }
        catch (Exception e) {
            CommonHelper.close(stream);
            throw new ScmSystemException("failed to create json reder", e);
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
        CommonHelper.close(stream);
        CommonHelper.closeNoLogging(jsonReader);
    }
}
