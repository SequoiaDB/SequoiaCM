package com.sequoiacm.infrastructrue.security.core;

public interface ITransaction {
    void begin() throws Exception;

    void commit();

    void rollback();

    ITransaction createTransation();
}
