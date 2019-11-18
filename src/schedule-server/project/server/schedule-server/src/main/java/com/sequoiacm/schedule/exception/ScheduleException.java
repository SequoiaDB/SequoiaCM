package com.sequoiacm.schedule.exception;

public class ScheduleException extends Exception {
    private static final long serialVersionUID = 1865008905278326611L;
    private String code;
    private String location;

    public ScheduleException(String code, String message, Throwable t) {
        super(message, t);
        this.code = code;
    }

    public ScheduleException(String code, String message) {
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
