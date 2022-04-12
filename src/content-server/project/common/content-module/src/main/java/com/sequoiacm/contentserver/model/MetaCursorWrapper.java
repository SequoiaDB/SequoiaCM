package com.sequoiacm.contentserver.model;

import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;

public abstract class MetaCursorWrapper<T> {
    private MetaCursor cursor;

    public MetaCursorWrapper(MetaCursor cursor) {
        this.cursor = cursor;
    }

    boolean hasNext() throws ScmMetasourceException {
        return cursor.hasNext();
    }

    T getNext() throws ScmMetasourceException {
        return convert(cursor.getNext());
    }

    void close() {
        cursor.close();
    }

    abstract T convert(BSONObject b);
}
