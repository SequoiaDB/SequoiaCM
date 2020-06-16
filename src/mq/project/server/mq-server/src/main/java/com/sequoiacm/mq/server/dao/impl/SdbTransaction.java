package com.sequoiacm.mq.server.dao.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.server.dao.Transaction;
import com.sequoiadb.base.Sequoiadb;

public class SdbTransaction implements Transaction {
    private static final Logger logger = LoggerFactory.getLogger(SdbTransaction.class);
    private Sequoiadb sdb;
    private SdbTemplate sdbTemplate;
    private boolean isBegin = false;
    private boolean isClose = false;

    @Autowired
    public SdbTransaction(SdbTemplate sdbTemplate) {
        this.sdbTemplate = sdbTemplate;
        this.sdb = sdbTemplate.getSequoiadb();
    }

    @Override
    public void begin() throws MqException {
        if (isBegin) {
            throw new MqException(MqError.SYSTEM_ERROR, "transaction begin twice");
        }
        if (isClose) {
            throw new MqException(MqError.SYSTEM_ERROR, "transaction has been close");
        }
        if (null != sdb) {
            try {
                sdb.beginTransaction();
                isBegin = true;
            }
            catch (Exception e) {
                releaseConnection();
                throw new MqException(MqError.METASOURCE_ERROR, "failed to begin transaction", e);
            }
        }
    }

    @Override
    public void commit() throws MqException {
        if (isClose) {
            throw new MqException(MqError.SYSTEM_ERROR, "transaction has been close");
        }
        if (null != sdb) {
            try {
                sdb.commit();
                isClose = true;
            }
            catch (Exception e) {
                throw new MqException(MqError.METASOURCE_ERROR, "failed to commit transaction", e);
            }
            finally {
                releaseConnection();
            }
        }
    }

    private void releaseConnection() {
        try {
            sdbTemplate.releaseSequoiadb(sdb);
        }
        catch (Exception e) {
            logger.warn("release connection failed:sdb={}", sdb, e);
        }

        sdb = null;
    }

    @Override
    public void rollback() {
        if (isClose) {
            logger.warn("transaction already close");
            return;
        }
        isClose = true;
        if (null != sdb) {
            try {
                sdb.rollback();
            }
            catch (Exception e) {
                logger.warn("failed to rollback transaction", e);
            }
            finally {
                releaseConnection();
            }
        }
    }

    Sequoiadb getSequoiadb() {
        if (!isBegin) {
            throw new RuntimeException("begin transaction first");
        }
        if (isClose) {
            throw new RuntimeException("transaction has been close");
        }
        return sdb;
    }

}
