package com.sequoiacm.deploy.exception;

public class DeployException extends ClusterException {
    private static final long serialVersionUID = -8497928334475036744L;

    public DeployException(String help, String error, Throwable causeby) {
        super(help, error, causeby);
    }
}
