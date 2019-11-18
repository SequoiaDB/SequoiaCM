package com.sequoiacm.config.metasource;

import org.bson.BSONObject;

import com.sequoiacm.config.metasource.exception.MetasourceException;

public interface MetaCursor {
    boolean hasNext() throws MetasourceException;

    BSONObject getNext() throws MetasourceException;

    void close();
}
