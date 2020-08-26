package com.sequoiacm.contentserver.exception;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;

public class ScmOperationUnsupportedException extends ScmServerException {

    public ScmOperationUnsupportedException(String message) {
        super(ScmError.OPERATION_UNSUPPORTED, message);
    }

    public ScmOperationUnsupportedException(String message, Throwable cause) {
        super(ScmError.OPERATION_UNSUPPORTED, message, cause);
    }
}
