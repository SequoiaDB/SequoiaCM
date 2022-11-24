package com.sequoiacm.infrastructure.lock.exception;

public class ScmLockTimeoutException extends Exception {

    public ScmLockTimeoutException(String message) {
        super(message);
    }

    public ScmLockTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
