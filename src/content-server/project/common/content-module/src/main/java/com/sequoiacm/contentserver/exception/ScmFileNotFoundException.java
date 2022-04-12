package com.sequoiacm.contentserver.exception;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;

public class ScmFileNotFoundException extends ScmServerException {

    public ScmFileNotFoundException(String message) {
        super(ScmError.FILE_NOT_FOUND, message);
    }

    public ScmFileNotFoundException(String message, Throwable cause) {
        super(ScmError.FILE_NOT_FOUND, message, cause);
    }
}
