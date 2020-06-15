package com.sequoiadb.infrastructure.map;

public class ScmMetasourceException extends ScmMapServerException {

    public ScmMetasourceException(String message) {
        super(ScmMapError.METASOURCE_ERROR, message);
    }

    public ScmMetasourceException(String message, Throwable cause) {
        super(ScmMapError.METASOURCE_ERROR, message, cause);
    }

}
