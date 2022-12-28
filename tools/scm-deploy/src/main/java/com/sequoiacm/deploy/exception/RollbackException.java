package com.sequoiacm.deploy.exception;

public class RollbackException extends ClusterException {
    public RollbackException(String error, Throwable causeby) {
        super(error, causeby);
    }
}
