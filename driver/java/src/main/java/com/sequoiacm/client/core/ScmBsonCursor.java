package com.sequoiacm.client.core;

import org.bson.BSONObject;

import com.sequoiacm.client.dispatcher.BsonReader;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonConverter;
import com.sequoiacm.client.util.ScmHelper;

class ScmBsonCursor<T> implements ScmCursor<T> {
    private BsonReader reader;
    private BsonConverter<T> converter;
    private boolean isClosed;

    ScmBsonCursor(BsonReader reader, BsonConverter<T> converter) {
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
    public T getNext() throws ScmException {
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
        ScmHelper.closeStream(reader);
        isClosed = true;
    }
}
