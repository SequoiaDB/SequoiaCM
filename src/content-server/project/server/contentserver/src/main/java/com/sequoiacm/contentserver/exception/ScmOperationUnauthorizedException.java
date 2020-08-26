package com.sequoiacm.contentserver.exception;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;

public class ScmOperationUnauthorizedException extends ScmServerException {

    public ScmOperationUnauthorizedException(String message) {
        super(ScmError.OPERATION_UNAUTHORIZED, message);
    }

    public ScmOperationUnauthorizedException(String message, Throwable cause) {
        super(ScmError.OPERATION_UNAUTHORIZED, message, cause);
    }
}
