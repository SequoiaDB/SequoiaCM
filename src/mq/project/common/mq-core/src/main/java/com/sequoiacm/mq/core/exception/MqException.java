package com.sequoiacm.mq.core.exception;

public class MqException extends Exception {

    private MqError error;

    public MqException(MqError error, String msg) {
        super(msg);
        this.error = error;
    }

    public MqException(MqError error, String msg, Throwable e) {
        super(msg, e);
        this.error = error;
    }

    public MqError getError() {
        return error;
    }
}
