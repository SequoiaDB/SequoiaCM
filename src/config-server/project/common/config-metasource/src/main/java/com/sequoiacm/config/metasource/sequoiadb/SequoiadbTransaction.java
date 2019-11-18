package com.sequoiacm.config.metasource.sequoiadb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiadb.base.Sequoiadb;

public class SequoiadbTransaction implements Transaction {
    private final static Logger logger = LoggerFactory.getLogger(SequoiadbTransaction.class);

    private Sequoiadb db;
    private SequoiadbMetasource metasurce;

    private boolean isBegin = false;

    public SequoiadbTransaction(SequoiadbMetasource metasource) throws MetasourceException {
        this.db = metasource.getConnection();
        this.metasurce = metasource;
        this.isBegin = false;
    }

    @Override
    public void begin() throws MetasourceException {
        try {
            db.beginTransaction();
            isBegin = true;
        }
        catch (Exception e) {
            throw new MetasourceException("failed to begin transaction", e);
        }
    }

    @Override
    public void commit() throws MetasourceException {
        if (!isBegin) {
            // do nothing if transaction is not started
            return;
        }

        try {
            db.commit();
            isBegin = false;
        }

        catch (Exception e) {
            throw new MetasourceException("failed to commit transaction", e);
        }
    }

    @Override
    public void rollback() {
        if (!isBegin) {
            // do nothing if transaction is not started
            return;
        }

        try {
            db.rollback();
            isBegin = false;
        }
        catch (Exception e) {
            logger.error("failed to rollback transaction", e);
        }
    }

    public Sequoiadb getConnection() {
        return db;
    }

    @Override
    public void close() {
        if (isBegin) {
            // make sure transaction is finished
            rollback();
        }

        metasurce.releaseConnection(db);
    }

    public boolean isBegin() {
        return isBegin;
    }
}
