package com.sequoiacm.sftp;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.exception.ScmError;

public class SftpDataException extends ScmDatasourceException {

    private static final long serialVersionUID = 810126604160820008L;
    private String errorCode = "";

    public static final String FILE_NOT_EXIST = "FileNotExist";
    public static final String FILE_EXIST = "FileExist";
    public static final String SFTP_ERROR_OPERATION_UNSUPPORTED = "OperationUnsupported";

    public SftpDataException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        setScmError();
    }

    public SftpDataException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        setScmError();
    }

    public SftpDataException(String message, Throwable e) {
        super(message, e);
    }

    public SftpDataException(String s) {
        super(s);
    }

    @Override
    public String toString() {
        return super.toString() + ", errorCode=" + getErrorCode();
    }

    public String getErrorCode() {
        return errorCode;
    }

    private void setScmError() {
        // data not exist
        if (FILE_NOT_EXIST.equals(errorCode)) {
            scmError = ScmError.DATA_NOT_EXIST;
        }
        // data exist
        else if (FILE_EXIST.equals(errorCode)) {
            scmError = ScmError.DATA_EXIST;
        }
        else if (SFTP_ERROR_OPERATION_UNSUPPORTED.equals(errorCode)) {
            scmError = ScmError.OPERATION_UNSUPPORTED;
        }
    }
}
