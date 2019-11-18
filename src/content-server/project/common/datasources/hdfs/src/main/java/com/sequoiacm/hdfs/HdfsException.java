package com.sequoiacm.hdfs;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.exception.ScmError;

public class HdfsException extends ScmDatasourceException {

    public static final String HDFS_ERROR_FILE_NOT_EXIST = "HDFS_ERROR_FILE_NOT_EXIST";
    public static final String HDFS_ERROR_FILE_ALREADY_EXISTS = "HDFS_ERROR_FILE_ALREADY_EXISTS";
    public static final String HDFS_ERROR_OPERATION_UNSUPPORTED = "HDFS_ERROR_OPERATION_UNSUPPORTED";

    private static final long serialVersionUID = -8048580650291559360L;

    private String errorCode = "";

    public HdfsException(String message, Throwable cause) {
        super(message, cause);
    }

    public HdfsException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        setScmError();
    }

    public HdfsException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        setScmError();
    }

    public HdfsException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return errorCode;
    }

    private void setScmError() {
        if (HDFS_ERROR_FILE_NOT_EXIST.equals(errorCode)) {
            scmError = ScmError.DATA_NOT_EXIST;
        }
        else if (HDFS_ERROR_FILE_ALREADY_EXISTS.equals(errorCode)) {
            scmError = ScmError.DATA_EXIST;
        }
        else if (HDFS_ERROR_OPERATION_UNSUPPORTED.equals(errorCode)) {
            scmError = ScmError.OPERATION_UNSUPPORTED;
        }
    }

    @Override
    public String toString() {
        if (!errorCode.equals("")) {
            return super.toString() + ",errorCode=" + errorCode;
        }
        else {
            return super.toString();
        }
    }
}
