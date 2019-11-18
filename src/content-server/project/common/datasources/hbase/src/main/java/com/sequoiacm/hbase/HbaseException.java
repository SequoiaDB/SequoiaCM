package com.sequoiacm.hbase;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.exception.ScmError;

public class HbaseException extends ScmDatasourceException {

    public static final String HBASE_ERROR_TABLE_NOTEXIST = "TableNotExist";
    public static final String HBASE_ERROR_FILE_NOTEXIST = "FileNotExist";
    public static final String HBASE_ERROR_FILE_EXIST = "FileExist";
    public static final String HBASE_ERROR_FILE_STATUS_UNAVAILABLE = "FileStatusUnavailable";
    public static final String HBASE_ERROR_FILE_CORRUPTED = "FileCorrupted";
    public static final String HBASE_ERROR_OPERATION_UNSUPPORTED = "OperationUnsupported";

    private static final long serialVersionUID = -6949043591285938411L;

    private String errorCode = "";

    public HbaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public HbaseException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        setScmError();
    }

    public HbaseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        setScmError();
    }

    public HbaseException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        if (!errorCode.equals("")) {
            return super.toString() + ",errorCode=" + errorCode;
        } else {
            return super.toString();
        }
    }

    private void setScmError() {
        if (HBASE_ERROR_TABLE_NOTEXIST.equals(errorCode)) {
            scmError =  ScmError.DATA_NOT_EXIST;
        } else if (HBASE_ERROR_FILE_NOTEXIST.equals(errorCode)) {
            scmError = ScmError.DATA_NOT_EXIST;
        } else if (HBASE_ERROR_FILE_STATUS_UNAVAILABLE.equals(errorCode)) {
            scmError = ScmError.DATA_UNAVAILABLE;
        } else if (HBASE_ERROR_FILE_CORRUPTED.equals(errorCode)) {
            scmError = ScmError.DATA_CORRUPTED;
        } else if (HBASE_ERROR_FILE_EXIST.equals(errorCode)) {
            scmError = ScmError.DATA_EXIST;
        } else if (HBASE_ERROR_OPERATION_UNSUPPORTED.equals(errorCode)) {
            scmError = ScmError.OPERATION_UNSUPPORTED;
        }
    }
}
