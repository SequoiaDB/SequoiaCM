package com.sequoiacm.client.exception;

import com.sequoiacm.exception.ScmError;

public class ScmSystemException extends ScmException {

    public ScmSystemException(String message, Throwable cause) {
        super(ScmError.SYSTEM_ERROR, message, cause);
    }

    public ScmSystemException(String message) {
        super(ScmError.SYSTEM_ERROR, message);
    }
}
