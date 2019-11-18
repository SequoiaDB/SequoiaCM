package com.sequoiacm.contentserver.exception;

import com.sequoiacm.exception.ScmError;

public class ScmFileExistException extends ScmServerException {

    public ScmFileExistException(String message) {
        super(ScmError.FILE_EXIST, message);
    }

    public ScmFileExistException(String message, Throwable cause) {
        super(ScmError.FILE_EXIST, message, cause);
    }
}
