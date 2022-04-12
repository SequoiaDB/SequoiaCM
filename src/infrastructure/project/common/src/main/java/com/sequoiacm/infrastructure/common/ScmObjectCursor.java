package com.sequoiacm.infrastructure.common;

import java.io.Closeable;

public interface ScmObjectCursor<T> extends Closeable {
    boolean hasNext() throws Exception;

    T getNext() throws Exception;

    void close();
}
