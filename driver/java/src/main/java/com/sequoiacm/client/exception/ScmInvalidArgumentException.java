package com.sequoiacm.client.exception;

import com.sequoiacm.exception.ScmError;

public class ScmInvalidArgumentException extends ScmException {

    public ScmInvalidArgumentException(String message, Throwable cause) {
        super(ScmError.INVALID_ARGUMENT, message, cause);
    }

    public ScmInvalidArgumentException(String message) {
        super(ScmError.INVALID_ARGUMENT, message);
    }
}
