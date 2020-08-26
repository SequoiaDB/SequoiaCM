package com.sequoiacm.contentserver.exception;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;

public class ScmOperationForbiddenException extends ScmServerException {

    public ScmOperationForbiddenException(String message) {
        super(ScmError.OPERATION_FORBIDDEN, message);
    }

    public ScmOperationForbiddenException(String message, Throwable cause) {
        super(ScmError.OPERATION_FORBIDDEN, message, cause);
    }
}
