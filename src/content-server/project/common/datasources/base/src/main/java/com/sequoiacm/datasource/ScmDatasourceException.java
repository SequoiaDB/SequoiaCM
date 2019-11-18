package com.sequoiacm.datasource;

import com.sequoiacm.exception.ScmError;

public class ScmDatasourceException extends Exception {
    private static final long serialVersionUID = 4858741630201019035L;

    protected ScmError scmError;

    public ScmDatasourceException(String message) {
        super(message);
    }

    public ScmDatasourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScmDatasourceException(ScmError scmError, String message) {
        super(message);
        this.scmError = scmError;
    }

    public ScmDatasourceException(ScmError scmError, String message, Throwable cause) {
        super(message, cause);
        this.scmError = scmError;
    }

    public ScmError getScmError(ScmError defaultScmError) {
        return scmError != null ? scmError : defaultScmError;
    }
}
