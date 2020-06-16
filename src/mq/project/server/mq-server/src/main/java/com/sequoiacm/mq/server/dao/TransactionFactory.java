package com.sequoiacm.mq.server.dao;

import com.sequoiacm.mq.core.exception.MqException;

public interface TransactionFactory {
    public Transaction createTransaction() throws MqException;
}
