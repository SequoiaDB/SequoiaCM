package com.sequoiacm.mq.server.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.server.dao.Transaction;
import com.sequoiacm.mq.server.dao.TransactionFactory;

@Component
public class SdbTransactionFactory implements TransactionFactory {
    private SdbTemplate sdbTemplate;

    @Autowired
    public SdbTransactionFactory(SdbTemplate sdbTemplate) {
        this.sdbTemplate = sdbTemplate;
    }

    @Override
    public Transaction createTransaction() throws MqException {
        return new SdbTransaction(sdbTemplate);
    }

}
