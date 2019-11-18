package com.sequoiacm.cloud.adminserver.exception;

public class ScmMetasourceException extends StatisticsException {

    private static final long serialVersionUID = -3849649977880892871L;

    public ScmMetasourceException(String msg, Throwable cause) {
        super(StatisticsError.METASOURCE_ERROR, msg, cause);
    }

    public ScmMetasourceException(String code, String msg, Throwable cause) {
        super(code, msg, cause);
    }

}
