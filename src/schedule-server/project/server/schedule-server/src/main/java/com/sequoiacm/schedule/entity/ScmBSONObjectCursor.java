package com.sequoiacm.schedule.entity;

import org.bson.BSONObject;

public interface ScmBSONObjectCursor {
    public boolean hasNext() throws Exception;

    public BSONObject next() throws Exception;

    public void close();
}
