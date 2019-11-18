package com.sequoiacm.cloud.adminserver.exception;

public class StatisticsException extends Exception {
    
    private static final long serialVersionUID = 2802973460805592072L;
    private String code;
    private String location;

    public StatisticsException(String code, String message, Throwable t) {
        super(message, t);
        this.code = code;
    }

    public StatisticsException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
