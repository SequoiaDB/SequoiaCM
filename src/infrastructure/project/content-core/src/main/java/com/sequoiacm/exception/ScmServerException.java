package com.sequoiacm.exception;

import com.sequoiacm.exception.ScmError;

public class ScmServerException extends Exception {

    private static final long serialVersionUID = 2254727849190358010L;

    protected ScmError error;

    public ScmServerException(ScmError error, String message, Throwable cause) {
        super(message, cause);
        if (error == null) {
            throw new NullPointerException("error is null");
        }
        this.error = error;
    }

    public ScmServerException(ScmError error, String message) {
        super(message);
        if (error == null) {
            throw new NullPointerException("error is null");
        }
        this.error = error;
    }

    public ScmError getError() {
        return error;
    }

    @Override
    public String toString() {
        return super.toString() + ", errorCode=" + error.getErrorCode();
    }

    public void resetError(ScmError error) {
        if (error == null) {
            throw new NullPointerException("error is null");
        }
        this.error = error;
    }
}
