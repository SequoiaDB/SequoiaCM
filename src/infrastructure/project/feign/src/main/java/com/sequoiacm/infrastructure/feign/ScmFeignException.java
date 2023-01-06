package com.sequoiacm.infrastructure.feign;

import java.util.Collection;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ScmFeignException extends Exception {
    private long timestamp;
    private int status;
    private String error;
    private String exception;
    private String message;
    private String path;

    @JsonIgnore
    private Map<String, Collection<String>> headers;

    public ScmFeignException() {
    }

    public ScmFeignException(HttpStatus status, String message) {
        this.timestamp = System.currentTimeMillis();
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.exception = getClass().getName();
        this.message = message;
    }

    public ScmFeignException(HttpStatus status, String message, Throwable cause) {
        super(cause);
        this.timestamp = System.currentTimeMillis();
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.exception = getClass().getName();
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setHeaders(Map<String, Collection<String>> headers) {
        this.headers = headers;
    }

    public Map<String, Collection<String>> getHeaders() {
        return headers;
    }
}
