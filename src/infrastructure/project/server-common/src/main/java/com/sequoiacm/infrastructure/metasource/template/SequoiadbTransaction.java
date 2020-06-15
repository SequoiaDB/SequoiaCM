package com.sequoiacm.infrastructure.metasource.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiadb.base.Sequoiadb;

public class SequoiadbTransaction implements ITransaction {
    private static final Logger logger = LoggerFactory.getLogger(SequoiadbTransaction.class);

    private DataSourceWrapper datasourceWrapper;
    private Sequoiadb sdb = null;

    public SequoiadbTransaction() {
        this.datasourceWrapper = DataSourceWrapper.getInstance();
    }

    @Override
    public void begin() throws Exception {
        if (null == sdb) {
            try {
                sdb = datasourceWrapper.getConnection();
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
            datasourceWrapper.releaseConnection(sdb);
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
            finally {
                releaseConnection();
            }
        }
    }

    public Sequoiadb getSequoiadb() {
        return sdb;
    }
}
