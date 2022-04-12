package com.sequoiacm.metasource.sequoiadb;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiadb.exception.SDBError;

public class SdbMetasourceException extends ScmMetasourceException {

    private int sdbErrcode;

    public SdbMetasourceException(int sdbErrcode, String message) {
        super(message);
        this.sdbErrcode = sdbErrcode;
        setScmError(mapToScmError(sdbErrcode));
    }

    public SdbMetasourceException(int sdbErrcode, String message, Throwable cause) {
        super(message, cause);
        this.sdbErrcode = sdbErrcode;
        setScmError(mapToScmError(sdbErrcode));
    }

    private ScmError mapToScmError(int sdbErrorcode) {
        if (sdbErrorcode == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
            return ScmError.METASOURCE_RECORD_EXIST;
        }
        else if (sdbErrorcode == SDBError.SDB_DMS_NOTEXIST.getErrorCode()
                || sdbErrorcode == SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()) {
            return ScmError.METASOURCE_TABLE_NOT_EXIST;
        }
        return ScmError.METASOURCE_ERROR;
    }

    @Override
    public String toString() {
        return super.toString() + ", sequoiadb errorCode=" + sdbErrcode;
    }

    public int getErrcode() {
        return sdbErrcode;
    }
}
