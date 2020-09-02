package com.sequoiadb.infrastructure.map;

public enum ScmMapError {
    UNKNOWN_ERROR(-1, "Unknown error"),
    SYSTEM_ERROR(-100, "System error"),
    INVALID_ARGUMENT(-101, "Invalid argument"),
    NETWORK_IO(-102, "Network IO Exception"),

    METASOURCE_ERROR(-200, "Metasource Error"),
    MAP_META_TABLE_NOT_EXIST(-240, "Map meta table not exist"),
    MAP_META_TABLE_ALREADY_EXIST(-241, "Map meta table already exist"),
    MAP_TABLE_NOT_EXIST(-250, "Map table not exist"),
    MAP_TABLE_ALREADY_EXIST(-251, "Map table already exist"),

    NUMBER_CROSS_BOUNDER(-300, "Number cross bounder"),
    PUT_CLASS_ERROR(-301, "Put type error"),
    MAP_CLASS_ERROR(-302, "Map type error");

    private int errorCode;
    private String desc;

    ScmMapError(int errorCode, String desc) {
        this.errorCode = errorCode;
        this.desc = desc;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return desc;
    }

    public String getErrorType() {
        return name();
    }

    @Override
    public String toString() {
        return name() + "(" + this.errorCode + ")" + ":" + this.desc;
    }

    public static ScmMapError getScmError(int errorCode) {
        for (ScmMapError value : ScmMapError.values()) {
            if (value.getErrorCode() == errorCode) {
                return value;
            }
        }

        return UNKNOWN_ERROR;
    }
}