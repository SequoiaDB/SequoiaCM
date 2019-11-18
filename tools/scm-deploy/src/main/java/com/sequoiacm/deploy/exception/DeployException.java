package com.sequoiacm.deploy.exception;

public class DeployException extends RuntimeException {
    private static final long serialVersionUID = -8497928334475036744L;
    private String help;

    public DeployException(String help, String error, Throwable causeby) {
        super(error, causeby);
        this.help = help;
    }

    public DeployException(String help, String msg) {
        super(msg);
        this.help = help;
    }

    public String getHelp() {
        return help;
    }
}
