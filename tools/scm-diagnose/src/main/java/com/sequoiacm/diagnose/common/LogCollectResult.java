package com.sequoiacm.diagnose.common;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class LogCollectResult {
    private int code;
    private String msg;

    private ScmToolsException exception;

    public LogCollectResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public LogCollectResult(int code, String msg, ScmToolsException exception) {
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

    public ScmToolsException getException() {
        return exception;
    }
}
