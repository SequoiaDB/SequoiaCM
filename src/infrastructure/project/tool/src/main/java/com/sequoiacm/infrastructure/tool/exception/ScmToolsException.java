package com.sequoiacm.infrastructure.tool.exception;

public class ScmToolsException extends Exception {

    /**
     * TODO
     */
    private static final long serialVersionUID = 1L;
    private int exitCode;
    private Object extra;

    public ScmToolsException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public ScmToolsException(String message, int exitCode, Object extra) {
        super(message);
        this.exitCode = exitCode;
        this.extra = extra;
    }

    public ScmToolsException(String message, int exitCode, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public ScmToolsException(int errorcode) {
        this.exitCode = errorcode;
    }

    public void printErrorMsg() {
        if (super.getMessage() != null && !super.getMessage().equals("")) {
            System.err.println(super.getMessage());
        }
    }

    public int getExitCode() {
        return this.exitCode;
    }

    public Object getExtra() {
        return extra;
    }
}
