package com.sequoiacm.cloud.adminserver.metasource;

import org.bson.BSONObject;

import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;

public interface MetaCursor {
    boolean hasNext() throws ScmMetasourceException;

    BSONObject getNext() throws ScmMetasourceException;

    void close();
}
