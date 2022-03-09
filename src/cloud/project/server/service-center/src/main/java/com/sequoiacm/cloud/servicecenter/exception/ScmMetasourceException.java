package com.sequoiacm.cloud.servicecenter.exception;

public class ScmMetasourceException extends ScmServiceCenterException {


    public ScmMetasourceException(String msg, Throwable cause) {
        super(ScmServiceCenterError.METASOURCE_ERROR, msg, cause);
    }

    public ScmMetasourceException(String code, String msg, Throwable cause) {
        super(code, msg, cause);
    }

}
