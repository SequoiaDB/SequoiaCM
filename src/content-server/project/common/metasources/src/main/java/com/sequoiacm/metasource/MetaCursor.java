package com.sequoiacm.metasource;

import org.bson.BSONObject;

import java.io.Closeable;

public interface MetaCursor extends Closeable {
    public boolean hasNext() throws ScmMetasourceException;
    public BSONObject getNext() throws ScmMetasourceException;
    public void close();
}
