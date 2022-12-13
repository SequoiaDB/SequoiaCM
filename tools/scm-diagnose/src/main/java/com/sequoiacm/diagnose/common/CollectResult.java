package com.sequoiacm.diagnose.common;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class CollectResult {
    private int code;
    private String msg;

    private Exception exception;

    public CollectResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public CollectResult(int code, String msg, Exception exception) {
        this.code = code;
        this.msg = msg;
        this.exception = exception;
    }

    public int getCode() {
        return code;
    }


    public String getMsg() {
        return msg;
    }

    public Exception getException() {
        return exception;
    }
}
