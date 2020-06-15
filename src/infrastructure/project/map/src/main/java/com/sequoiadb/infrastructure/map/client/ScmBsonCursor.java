package com.sequoiadb.infrastructure.map.client;

import org.bson.BSONObject;

import com.sequoiadb.infrastructure.map.CommonHelper;
import com.sequoiadb.infrastructure.map.ScmMapServerException;

public class ScmBsonCursor<T> implements ScmCursor<T> {
    private BsonReader reader;
    private BsonConverter<T> converter;
    private boolean isClosed;

    public ScmBsonCursor(BsonReader reader, BsonConverter<T> converter) {
        this.reader = reader;
        this.converter = converter;
        this.isClosed = false;
    }

    @Override
    public boolean hasNext() {
        if (isClosed) {
            return false;
        }
        return reader.hasNext();
    }

    @Override
    public T getNext() throws ScmMapServerException {
        if (!isClosed && reader.hasNext()) {
            BSONObject obj = reader.getNext();
            return converter.convert(obj);
        }
        else {
            return null;
        }
    }

    @Override
    public void close() {
        CommonHelper.close(reader);
        isClosed = true;
    }
}
