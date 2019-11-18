package com.sequoiacm.contentserver.exception;

import com.sequoiacm.exception.ScmError;

public class ScmLockException extends ScmServerException {

    public ScmLockException(String message, Throwable cause) {
        super(ScmError.LOCK_ERROR, message, cause);
    }

    public ScmLockException(String message) {
        super(ScmError.LOCK_ERROR, message);
    }
}
