package com.sequoiacm.om.omserver.exception;

public enum ScmOmServerError {
    SYSTEM_ERROR(500, "system error"),

    SESSION_NOTEXIST(401, "session not exist"),
    UNSUPPORT_OPERATION(403, "unsupport operation"),

    INVALID_ARGUMENT(400, "Invalid argument");

    private int httpStatusCode;
    private String desc;

    private ScmOmServerError(int httpStatusCode, String desc) {
        this.httpStatusCode = httpStatusCode;
        this.desc = desc;
    }

    public int getErrorCode() {
        return httpStatusCode;
    }

    public String getErrorDescription() {
        return desc;
    }

    @Override
    public String toString() {
        return name() + "(" + this.httpStatusCode + ")" + ":" + this.desc;
    }
}
