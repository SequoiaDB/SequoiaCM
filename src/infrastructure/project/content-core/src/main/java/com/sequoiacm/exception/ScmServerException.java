package com.sequoiacm.exception;

import org.bson.BSONObject;

public class ScmServerException extends Exception {

    private static final long serialVersionUID = 2254727849190358010L;

    protected ScmError error;

    // 支持跨节点传输，即A节点抛出异常携带 extraInfo，B节点捕获这个异常也能看到这个 extraInfo
    protected BSONObject extraInfo;

    public ScmServerException(ScmError error, String message, Throwable cause) {
        super(message, cause);
        if (error == null) {
            throw new NullPointerException("error is null");
        }
        this.error = error;
    }

    public ScmServerException(ScmError error, String message, Throwable cause,
            BSONObject extraInfo) {
        super(message, cause);
        if (error == null) {
            throw new NullPointerException("error is null");
        }
        this.error = error;
        this.extraInfo = extraInfo;
    }

    public ScmServerException(ScmError error, String message) {
        super(message);
        if (error == null) {
            throw new NullPointerException("error is null");
        }
        this.error = error;
    }

    public ScmServerException(ScmError error, String message, BSONObject extraInfo) {
        super(message);
        if (error == null) {
            throw new NullPointerException("error is null");
        }
        this.error = error;
        this.extraInfo = extraInfo;
    }
    public ScmError getError() {
        return error;
    }

    @Override
    public String toString() {
        return super.toString() + ", errorCode=" + error.getErrorCode();
    }

    public void resetError(ScmError error) {
        if (error == null) {
            throw new NullPointerException("error is null");
        }
        this.error = error;
    }

    public BSONObject getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(BSONObject extraInfo) {
        this.extraInfo = extraInfo;
    }
}
