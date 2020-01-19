package com.sequoiacm.sequoiadb;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.exception.ScmError;
import com.sequoiadb.exception.SDBError;

public class SequoiadbException extends ScmDatasourceException {

    private static final long serialVersionUID = 810126604160820005L;
    private int dbError;

    public SequoiadbException(int dbError, String message, Throwable cause) {
        super(message, cause);
        this.dbError = dbError;
        setScmError();
    }

    public SequoiadbException(int dbError, String message) {
        super(message);
        this.dbError = dbError;
        setScmError();
    }

    public SequoiadbException(String message, Throwable e) {
        super(message, e);
    }

    @Override
    public String toString() {
        return super.toString() + ", sequoiadb errorCode=" + getDatabaseError();
    }

    public int getDatabaseError() {
        return dbError;
    }

    private void setScmError() {
        // data not exist
        if (dbError == SDBError.SDB_DMS_NOTEXIST.getErrorCode()
                || dbError == SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()
                || dbError == SDBError.SDB_FNE.getErrorCode()) {
            scmError = ScmError.DATA_NOT_EXIST;
        }
        // data unavailable
        else if (dbError == SDBError.SDB_LOB_IS_NOT_AVAILABLE.getErrorCode()) {
            scmError = ScmError.DATA_UNAVAILABLE;
        }
        // data exist
        else if (dbError == SDBError.SDB_FE.getErrorCode()) {
            scmError = ScmError.DATA_EXIST;
        }
        //data is corrupted
        else if (dbError == SDBError.SDB_LOB_SEQUENCE_NOT_EXIST.getErrorCode()) {
            scmError = ScmError.DATA_CORRUPTED;
        }

        else if (dbError == SDBError.SDB_LOB_IS_IN_USE.getErrorCode()) {
            scmError = ScmError.DATA_IS_IN_USE;
        }
    }
}
