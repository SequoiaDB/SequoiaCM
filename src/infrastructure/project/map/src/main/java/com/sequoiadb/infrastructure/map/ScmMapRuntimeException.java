package com.sequoiadb.infrastructure.map;

public class ScmMapRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 2254727849190358010L;

    protected ScmMapError error;

    public ScmMapRuntimeException(ScmMapServerException e) {
        super(e);
        this.error = e.getError();
        if (error == null) {
            throw new NullPointerException("error is null");
        }

    }

    public ScmMapRuntimeException(String message, ScmMapServerException e) {
        super(message, e);
        this.error = e.getError();
        if (error == null) {
            throw new NullPointerException("error is null");
        }

    }

    public ScmMapRuntimeException(ScmMapError error, String message, Throwable cause) {
        super(message, cause);
        if (error == null) {
            throw new NullPointerException("error is null");
        }
        this.error = error;
    }

    public ScmMapRuntimeException(ScmMapError error, String message) {
        super(message);
        if (error == null) {
            throw new NullPointerException("error is null");
        }
        this.error = error;
    }

    public ScmMapRuntimeException(int errorCode, String message) {
        super(message);
        this.error = ScmMapError.getScmError(errorCode);
    }

    public ScmMapError getError() {
        return error;
    }

    @Override
    public String toString() {
        return super.toString() + ", errorCode=" + error.getErrorCode();
    }

    public void resetError(ScmMapError error) {
        if (error == null) {
            throw new NullPointerException("error is null");
        }
        this.error = error;
    }
}
