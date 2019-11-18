package com.sequoiacm.metasource;

import com.sequoiacm.exception.ScmError;

public class ScmMetasourceException extends Exception {

    private ScmError scmError = ScmError.METASOURCE_ERROR;

    public ScmMetasourceException(String message) {
        super(message);
    }

    public ScmMetasourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScmError getScmError() {
        return scmError;
    }

    public void setScmError(ScmError scmError) {
        this.scmError = scmError;
    }
}
