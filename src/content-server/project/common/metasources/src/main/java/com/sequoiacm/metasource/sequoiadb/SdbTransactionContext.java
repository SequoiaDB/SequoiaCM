package com.sequoiacm.metasource.sequoiadb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

public class SdbTransactionContext implements TransactionContext {
    private final static Logger logger = LoggerFactory.getLogger(SdbTransactionContext.class);

    private Sequoiadb db;
    private SdbMetaSource metasurce;

    private boolean isBegin = false;

    public SdbTransactionContext(SdbMetaSource metasource) throws SdbMetasourceException {
        this.db = metasource.getConnection();
        this.metasurce = metasource;
        this.isBegin = false;
    }

    @Override
    public void begin() throws SdbMetasourceException {
        try {
            db.beginTransaction();
            isBegin = true;
        }
        catch (BaseException e) {
            throw new SdbMetasourceException(e.getErrorCode(), "failed to begin transaction", e);
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "failed to begin transaction", e);
        }
    }

    @Override
    public void commit() throws SdbMetasourceException {
        if (!isBegin) {
            // do nothing if transaction is not started
            return;
        }

        try {
            db.commit();
            isBegin = false;
        }
        catch (BaseException e) {
            SdbMetasourceException msEx = new SdbMetasourceException(e.getErrorCode(),
                    "failed to commit transaction", e);
            msEx.setScmError(ScmError.COMMIT_UNCERTAIN_STATE);
            throw msEx;
        }
        catch (Exception e) {
            SdbMetasourceException msEx = new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "failed to commit transaction", e);
            msEx.setScmError(ScmError.COMMIT_UNCERTAIN_STATE);
            throw msEx;
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
