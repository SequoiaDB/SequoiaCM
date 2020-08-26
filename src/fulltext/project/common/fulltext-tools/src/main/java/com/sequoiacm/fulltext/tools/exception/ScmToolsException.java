package com.sequoiacm.fulltext.tools.exception;

public class ScmToolsException extends Exception {

    /**
     * TODO
     */
    private static final long serialVersionUID = 1L;
    private int exitCode;

    public ScmToolsException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
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
}
