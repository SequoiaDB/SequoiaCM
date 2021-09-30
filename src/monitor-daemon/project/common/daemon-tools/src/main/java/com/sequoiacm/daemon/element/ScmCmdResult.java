package com.sequoiacm.daemon.element;

import java.util.List;

public class ScmCmdResult {
    private int rc;
    private List<String> stdErr;
    private List<String> stdIn;

    public ScmCmdResult() {
    }

    public int getRc() {
        return rc;
    }

    public void setRc(int rc) {
        this.rc = rc;
    }

    public List<String> getStdErr() {
        return stdErr;
    }

    public void setStdErr(List<String> stdErr) {
        this.stdErr = stdErr;
    }

    public List<String> getStdIn() {
        return stdIn;
    }

    public void setStdIn(List<String> stdIn) {
        this.stdIn = stdIn;
    }

    public String getStdStr() {
        StringBuilder sb = new StringBuilder();
        for (String s : stdErr) {
            sb.append(s);
            sb.append("\n");
        }
        for (String s : stdIn) {
            sb.append(s);
            sb.append("\n");
        }
        String message = sb.toString();
        if (message.endsWith("\n")) {
            message = message.substring(0, message.length() - 1);
        }
        return message;
    }
}
