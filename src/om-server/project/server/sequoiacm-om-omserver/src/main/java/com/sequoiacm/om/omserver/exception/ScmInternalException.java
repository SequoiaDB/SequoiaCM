package com.sequoiacm.om.omserver.exception;

import org.springframework.http.HttpStatus;

import com.sequoiacm.exception.ScmError;

public class ScmInternalException extends Exception {
    private int errorCode;
    private HttpStatus status;

    public ScmInternalException(ScmError errorCode, String errorMsg, Throwable cause) {
        super(errorMsg, cause);
        this.errorCode = errorCode.getErrorCode();

        this.status = null;
        switch (errorCode) {
            case MISSING_ARGUMENT:
            case INVALID_ARGUMENT:
            case INVALID_ID:
                status = HttpStatus.BAD_REQUEST;
                break;
            case OPERATION_FORBIDDEN:
                status = HttpStatus.FORBIDDEN;
                break;
            case OPERATION_UNAUTHORIZED:
                status = HttpStatus.UNAUTHORIZED;
                break;
            case FILE_NOT_FOUND:
            case DIR_NOT_FOUND:
            case FILE_NOT_EXIST:
            case SITE_NOT_EXIST:
            case TASK_NOT_EXIST:
                status = HttpStatus.NOT_FOUND;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

    }

    public ScmInternalException(int errorCode, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public int getStatus() {
        return status.value();
    }
}
