package com.sequoiacm.metasource.sequoiadb;

import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiadb.exception.SDBError;

public class SdbMetasourceException extends ScmMetasourceException {

    private int errcode;

    public SdbMetasourceException(int errcode, String message) {
        super(message);
        this.errcode = errcode;
    }

    public SdbMetasourceException(int errcode, String message, Throwable cause) {
        super(message, cause);
        this.errcode = errcode;
    }

    public SdbMetasourceException(String message) {
        super(message);
        this.errcode = SDBError.SDB_SYS.getErrorCode();
    }

    @Override
    public String toString() {
        return super.toString() + ", sequoiadb errorCode=" + errcode;
    }

    public int getErrcode() {
        return errcode;
    }
}
