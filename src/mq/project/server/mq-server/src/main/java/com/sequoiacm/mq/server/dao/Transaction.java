package com.sequoiacm.mq.server.dao;

import com.sequoiacm.mq.core.exception.MqException;

public interface Transaction {
    void begin() throws MqException;

    void commit() throws MqException;

    void rollback();

}
