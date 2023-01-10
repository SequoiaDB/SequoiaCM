package com.sequoiacm.schedule.dao.sequoiadb;

import com.sequoiacm.schedule.dao.Transaction;
import com.sequoiadb.base.Sequoiadb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionImpl implements Transaction {
    private static final Logger logger = LoggerFactory.getLogger(TransactionImpl.class);

    private SdbDataSourceWrapper datasource;

    private Sequoiadb sdb;

    public TransactionImpl(SdbDataSourceWrapper datasource) {
        this.datasource = datasource;
    }

    @Override
    public void begin() throws Exception {
        if (null == sdb) {
            try {
                sdb = datasource.getConnection();
                sdb.beginTransaction();
            }
            catch (Exception e) {
                releaseConnection();
                throw e;
            }
        }
    }

    @Override
    public void commit() {
        if (null != sdb) {
            try {
                sdb.commit();
            }
            finally {
                releaseConnection();
            }
        }
    }

    private void releaseConnection() {
        try {
            datasource.releaseConnection(sdb);
        }
        catch (Exception e) {
            logger.warn("release connection failed:sdb={}", sdb, e);
        }

        sdb = null;
    }

    @Override
    public void rollback() {
        if (null != sdb) {
            try {
                sdb.rollback();
            }
            catch (Exception e) {
                logger.error("rollback failed:sdb={}", sdb, e);
            }
            finally {
                releaseConnection();
            }
        }
    }

    public Sequoiadb getSequoiadb() {
        return sdb;
    }
}
