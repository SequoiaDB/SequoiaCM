package com.sequoiacm.s3.scan;

import com.sequoiacm.metasource.ScmMetasourceException;

public interface RecordWrapperCursor<T extends RecordWrapper> {
    boolean hasNext() throws ScmMetasourceException;

    T getNext() throws ScmMetasourceException;

    void close();
}
