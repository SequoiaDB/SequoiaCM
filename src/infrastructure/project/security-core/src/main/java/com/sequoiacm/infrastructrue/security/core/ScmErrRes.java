package com.sequoiacm.infrastructrue.security.core;

public class ScmErrRes {

    private long timestamp;
    private int status;
    private String error;
    private String exception;
    private String message;
    private String path;

    public ScmErrRes() {
    }

    public ScmErrRes(long timestamp, int status, String error, String exception, String message,
            String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.exception = exception;
        this.message = message;
        this.path = path;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getException() {
        return exception;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

}