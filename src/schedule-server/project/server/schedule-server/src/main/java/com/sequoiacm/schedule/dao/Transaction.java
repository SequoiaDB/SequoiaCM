package com.sequoiacm.schedule.dao;

public interface Transaction {
    void begin() throws Exception;

    void commit();

    void rollback();
}
