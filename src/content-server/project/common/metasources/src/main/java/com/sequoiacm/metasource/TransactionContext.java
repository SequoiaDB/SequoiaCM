package com.sequoiacm.metasource;

public interface TransactionContext {
    public void begin() throws ScmMetasourceException;

    public void commit() throws ScmMetasourceException;

    public void rollback();

    public void close();
}
