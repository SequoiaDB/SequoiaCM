package com.sequoiacm.infrastructure.config.core.exception;

public class ScmConfigException extends Exception {
    private ScmConfError error;

    public ScmConfigException(ScmConfError error, String msg, Throwable cause) {
        super(msg, cause);
        this.error = error;
    }

    public ScmConfigException(ScmConfError error, String msg) {
        super(msg);
        this.error = error;
    }

    public ScmConfError getError() {
        return error;
    }

    @Override
    public String toString() {
        return super.toString() + ", error=" + error;
    }
}
