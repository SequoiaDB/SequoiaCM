package com.sequoiacm.test.module;

public class ExecResult {

    private int exitCode;
    private String stdOut;
    private String stdErr;

    public ExecResult(int exitCode, String stdOut, String stdErr) {
        this.exitCode = exitCode;
        this.stdOut = stdOut;
        this.stdErr = stdErr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdOut() {
        return stdOut;
    }

    public String getStdErr() {
        return stdErr;
    }

    @Override
    public String toString() {
        return "exitCode=" + exitCode + ", stdOut='" + stdOut + '\'' + ", stdErr='" + stdErr;
    }
}
