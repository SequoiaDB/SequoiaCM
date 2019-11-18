package com.sequoiacm.infrastructrue.security.core;

public class ScmErrException extends Exception {

    private static final long serialVersionUID = 2254727849190358010L;
    protected int errorCode = -1;

    public ScmErrException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ScmErrException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return super.toString() + ", errorCode=" + errorCode;
    }

    public void resetErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
}