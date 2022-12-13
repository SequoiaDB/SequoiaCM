package com.sequoiacm.diagnose.common;

public class SshExecRes {
    private int exitCode;
    private String stdOut;
    private String stdErr;

    public SshExecRes(int exitCode, String stdOut, String stdErr) {
        super();
        this.exitCode = exitCode;
        this.stdOut = stdOut;
        this.stdErr = stdErr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdErr() {
        return stdErr;
    }

    public String getStdOut() {
        return stdOut;
    }

    @Override
    public String toString() {
        return "SshExecRes{" + "exitCode=" + exitCode + ",stdOut='" + stdOut + '\'' + ",stdErr='"
                + stdErr + '\'' + '}';
    }
}
