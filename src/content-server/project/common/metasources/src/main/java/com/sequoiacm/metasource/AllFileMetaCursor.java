package com.sequoiacm.metasource;

import org.bson.BSONObject;

public class AllFileMetaCursor implements MetaCursor {
    protected MetaCursor currentFileCursor;
    protected MetaCursor historyFileCursor;

    public AllFileMetaCursor(MetaCursor currentFileCursor, MetaCursor historyFileCursor)
            throws ScmMetasourceException {
        this.currentFileCursor = currentFileCursor;
        this.historyFileCursor = historyFileCursor;
    }

    @Override
    public boolean hasNext() throws ScmMetasourceException {
        return historyFileCursor.hasNext() || currentFileCursor.hasNext();
    }

    @Override
    public BSONObject getNext() throws ScmMetasourceException {
        BSONObject c = currentFileCursor.getNext();
        if (c != null) {
            return c;
        }
        return historyFileCursor.getNext();
    }

    @Override
    public void close() {
        if (historyFileCursor != null) {
            historyFileCursor.close();
        }

        if (currentFileCursor != null) {
            currentFileCursor.close();
        }

    }
}
