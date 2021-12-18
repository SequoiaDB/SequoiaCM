package com.sequoiacm.infrastructure.feign.hystrix;

public class ScmHystrixIgnoreException extends Exception {

    public ScmHystrixIgnoreException(Throwable cause) {
        super(cause);
    }
}
