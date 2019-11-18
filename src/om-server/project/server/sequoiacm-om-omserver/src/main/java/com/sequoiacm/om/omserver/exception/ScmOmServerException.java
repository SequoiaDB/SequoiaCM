package com.sequoiacm.om.omserver.exception;

public class ScmOmServerException extends Exception {
    private ScmOmServerError error;

    public ScmOmServerException(ScmOmServerError error, String msg, Throwable cause) {
        super(msg, cause);
        this.error = error;
    }

    public ScmOmServerException(ScmOmServerError error, String msg) {
        super(msg);
        this.error = error;
    }

    public ScmOmServerError getError() {
        return error;
    }

    @Override
    public String toString() {
        return super.toString() + ", error=" + error;
    }
}
