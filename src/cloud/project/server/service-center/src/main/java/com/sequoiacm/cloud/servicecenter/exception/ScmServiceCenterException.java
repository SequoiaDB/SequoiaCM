package com.sequoiacm.cloud.servicecenter.exception;

public class ScmServiceCenterException extends Exception {

    private String code;

    public ScmServiceCenterException(String code, String message, Throwable t) {
        super(message, t);
        this.code = code;
    }

    public ScmServiceCenterException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}
