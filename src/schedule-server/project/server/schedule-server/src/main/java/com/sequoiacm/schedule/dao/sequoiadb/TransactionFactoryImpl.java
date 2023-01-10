package com.sequoiacm.schedule.dao.sequoiadb;

import com.sequoiacm.schedule.dao.Transaction;
import com.sequoiacm.schedule.dao.TransactionFactory;
import org.springframework.stereotype.Component;

@Component
public class TransactionFactoryImpl implements TransactionFactory {

    private SdbDataSourceWrapper datasource;

    public TransactionFactoryImpl(SdbDataSourceWrapper datasource) {
        this.datasource = datasource;
    }

    @Override
    public Transaction createTransaction() {
        return new TransactionImpl(datasource);
    }
}
