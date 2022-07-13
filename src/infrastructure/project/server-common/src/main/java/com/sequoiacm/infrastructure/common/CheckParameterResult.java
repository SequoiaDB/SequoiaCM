package com.sequoiacm.infrastructure.common;

public class CheckParameterResult {
    boolean isSuccessful;
    String msg;

    public CheckParameterResult(boolean isSuccessful, String msg) {
        this.isSuccessful = isSuccessful;
        this.msg = msg;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public String getMsg() {
        return msg;
    }
}