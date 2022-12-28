package com.sequoiacm.deploy.exception;

public class UpgradeException extends ClusterException {
    public UpgradeException(String error, Throwable causeby) {
        super(error, causeby);
    }

    public UpgradeException(String error) {
        super(error);
    }
}
