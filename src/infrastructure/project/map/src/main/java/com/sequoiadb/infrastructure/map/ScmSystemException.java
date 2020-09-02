package com.sequoiadb.infrastructure.map;

public class ScmSystemException extends ScmMapServerException {
    public ScmSystemException(String message) {
        super(ScmMapError.SYSTEM_ERROR, message);
    }

    public ScmSystemException(String message, Throwable cause) {
        super(ScmMapError.SYSTEM_ERROR, message, cause);
    }
}
