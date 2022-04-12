package com.sequoiacm.metasource;

import org.bson.BSONObject;

import com.sequoiacm.infrastructure.common.ScmObjectCursor;

public interface MetaCursor extends ScmObjectCursor<BSONObject> {
    public boolean hasNext() throws ScmMetasourceException;
    public BSONObject getNext() throws ScmMetasourceException;
    public void close();
}
