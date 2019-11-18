package com.sequoiacm.contentserver.exception;

import com.sequoiacm.exception.ScmError;

public class ScmInvalidArgumentException extends ScmServerException {

    public ScmInvalidArgumentException(String message) {
        super(ScmError.INVALID_ARGUMENT, message);
    }

    public ScmInvalidArgumentException(String message, Throwable cause) {
        super(ScmError.INVALID_ARGUMENT, message, cause);
    }
}
