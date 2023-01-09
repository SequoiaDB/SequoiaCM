package com.sequoiacm.fulltext.server.exception;

import com.sequoiacm.exception.ScmError;

public class FullTextException extends Exception {

    private ScmError error;

    public FullTextException(ScmError error, String msg) {
        super(msg);
        this.error = error;
    }

    public FullTextException(ScmError error, String msg, Throwable e) {
        super(msg, e);
        this.error = error;
    }

    public ScmError getError() {
        return error;
    }
}
