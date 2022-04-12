package com.sequoiacm.config.server.service;

import org.bson.BSONObject;

import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.config.metasource.exception.MetasourceException;

public abstract class MetaCursorWrapper<T> {
    private MetaCursor cursor;

    public MetaCursorWrapper(MetaCursor c) {
        this.cursor = c;
    }

    public boolean hasNext() throws MetasourceException {
        return cursor.hasNext();
    }

    public T getNext() throws MetasourceException {
        return convert(cursor.getNext());
    }

    abstract T convert(BSONObject r);
}
