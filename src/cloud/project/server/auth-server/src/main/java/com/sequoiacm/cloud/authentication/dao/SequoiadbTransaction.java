package com.sequoiacm.cloud.authentication.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.infrastructrue.security.core.ITransaction;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;

@Repository("ITransaction")
public class SequoiadbTransaction implements ITransaction {
    private static final Logger logger = LoggerFactory.getLogger(SequoiadbTransaction.class);

    private SequoiadbDatasource datasource;
    private Sequoiadb sdb = null;

    @Autowired
    public SequoiadbTransaction(SequoiadbDatasource datasource) {
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
            finally {
                releaseConnection();
            }
        }
    }

    @Override
    public ITransaction createTransation() {
        return new SequoiadbTransaction(datasource);
    }

    public Sequoiadb getSequoiadb() {
        return sdb;
    }

}
