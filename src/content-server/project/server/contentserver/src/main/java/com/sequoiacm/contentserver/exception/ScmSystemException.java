package com.sequoiacm.contentserver.exception;

import com.sequoiacm.exception.ScmError;

public class ScmSystemException extends ScmServerException {

    public ScmSystemException(String message) {
        super(ScmError.SYSTEM_ERROR, message);
    }

    public ScmSystemException(String message, Throwable cause) {
        super(ScmError.SYSTEM_ERROR, message, cause);
    }
}
