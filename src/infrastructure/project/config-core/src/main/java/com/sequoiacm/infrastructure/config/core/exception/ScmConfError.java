package com.sequoiacm.infrastructure.config.core.exception;

public enum ScmConfError {
    UNKNOWN_ERROR(-1, "unknown error"),
    SYSTEM_ERROR(-2, "system error"),

    CONFIG_ERROR(-100, "config error"),

    METASOURCE_ERROR(-200, "metasource error"),
    METASOURCE_RECORD_EXIST(-201, "record exist"),
    METASOURCE_TABLE_NOT_EXIST(-202, "table not exist"),

    LOCK_ERROR(-300, "lock error"),

    INVALID_ARG(-400, "invlid argument"),

    NO_SUCH_CONFIG(-401, "no such config"),
    UNSUPPORTED_OPTION(-402, "unsupported option"),

    // workspace related
    WORKSPACE_EXIST(-500, "workspace exist"),
    CLIENT_WROKSPACE_CACHE_EXPIRE(-501, "client workspace cache is expire"),
    WORKSPACE_NOT_EXIST(-502, "workspace not exist"),

    // metadata related
    CLASS_EXIST(-600, "class exist"),
    CLASS_NOT_EXIST(-601, "class not exist"),
    ATTRIBUTE_EXIST(-602, "attribute exist"),
    ATTRIBUTE_NOT_EXIST(-603, "attribute not exist"),
    ATTRIBUTE_ALREADY_IN_CLASS(-604, "Class is already attached with this attr"),
    ATTRIBUTE_NOT_IN_CLASS(-605, "Class is not attached with this attr"),
    ATTRIBUTE_IN_CLASS(-606, "Attribute are attached with certain classes"),

    // site related
    SITE_EXIST(-700, "site exist"),
    SITE_NOT_EXIST(-701, "site not exist"),
    DATASOURCE_EXIST_OTHER_SITE(-702, "datasource exist in other site"),

    // node related
    NODE_EXIST(-800, "node exist"),
    NODE_NOT_EXIST(-801, "node not exist"),

    BUCKET_EXIST(-900, "bucket exist"),

    BUCKET_NOT_EXIST(-901, "bucket not exist"),

    // quota related
    QUOTA_EXIST(-1000, "quota exist"),
    QUOTA_NOT_EXIST(-1001, "quota not exist");

    private int errorCode;
    private String desc;

    private ScmConfError(int errorCode, String desc) {
        this.errorCode = errorCode;
        this.desc = desc;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return desc;
    }

    @Override
    public String toString() {
        return name() + "(" + this.errorCode + ")" + ":" + this.desc;
    }

    public static ScmConfError getScmError(int errorCode) {
        for (ScmConfError value : ScmConfError.values()) {
            if (value.getErrorCode() == errorCode) {
                return value;
            }
        }

        return UNKNOWN_ERROR;
    }
}
