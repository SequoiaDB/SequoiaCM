package com.sequoiacm.infrastructure.feign.hystrix;

public class ScmHystrixException extends Exception {
    public ScmHystrixException(String message, Throwable cause) {
        super(message, cause);
    }
}
