package com.sequoiacm.infrastructure.lock.exception;

public class ScmLockException extends Exception {

    public ScmLockException(String message) {
        super(message);
    }

    public ScmLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
