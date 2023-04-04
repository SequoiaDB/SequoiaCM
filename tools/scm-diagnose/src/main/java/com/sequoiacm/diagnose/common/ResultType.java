package com.sequoiacm.diagnose.common;

public enum ResultType {
    SAME("SAME"),
    ERR_SIZE("ERR_SIZE"),
    ERR_MD5("ERR_MD5"),
    FAILED("FAILED"),
    UNKNOWN("unknown");

    ResultType(String type) {
        this.type = type;
    }

    private String type;

    public String getType() {
        return type;
    }

    public static ResultType getType(String val) {
        for (ResultType type : ResultType.values()) {
            if (type.getType().equals(val)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
