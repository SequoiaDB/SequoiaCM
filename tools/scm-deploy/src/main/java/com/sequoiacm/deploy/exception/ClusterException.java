package com.sequoiacm.deploy.exception;

public class ClusterException extends RuntimeException {
    private String help;

    public ClusterException(String help, String error, Throwable causeby) {
        super(error, causeby);
        this.help = help;
    }

    public ClusterException(String error, Throwable causeby) {
        super(error, causeby);
    }

    public ClusterException(String error) {
        super(error);
    }

    public String getHelp() {
        return help;
    }
}
