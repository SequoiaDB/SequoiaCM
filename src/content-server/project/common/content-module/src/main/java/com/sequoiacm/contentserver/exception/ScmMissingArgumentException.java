package com.sequoiacm.contentserver.exception;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;

public class ScmMissingArgumentException extends ScmServerException {

    public ScmMissingArgumentException(String message) {
        super(ScmError.MISSING_ARGUMENT, message);
    }
}
