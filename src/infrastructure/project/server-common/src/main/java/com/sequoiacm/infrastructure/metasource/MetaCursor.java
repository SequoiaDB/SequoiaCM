package com.sequoiacm.infrastructure.metasource;

import org.bson.BSONObject;

public interface MetaCursor {
    public boolean hasNext();

    public BSONObject getNext();

    public void close();
}
