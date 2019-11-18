package com.sequoiacm.cloud.authentication.exception;

import org.springframework.http.HttpStatus;

public class RestException extends RuntimeException {
    private long timestamp;
    private HttpStatus status;
    private String exception;
    private String message;
    private String path;

    public RestException(HttpStatus status, String message) {
        this.timestamp = System.currentTimeMillis();
        this.status = status;
        this.exception = getClass().getName();
        this.message = message;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
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

    public long getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status.value();
    }

    public String getError() {
        return status.getReasonPhrase();
    }

    public String getException() {
        return exception;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return String.format("{\"timestamp\":%d,\"status\":%d,\"error\":\"%s\",\"exception\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                timestamp, getStatus(), getError(), exception, message, path);
    }
}
