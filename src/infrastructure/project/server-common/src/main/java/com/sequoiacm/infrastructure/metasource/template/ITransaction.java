package com.sequoiacm.infrastructure.metasource.template;

public interface ITransaction {
    void begin() throws Exception;

    void commit();

    void rollback();
}
