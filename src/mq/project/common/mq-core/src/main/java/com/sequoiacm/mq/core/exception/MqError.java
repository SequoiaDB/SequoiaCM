package com.sequoiacm.mq.core.exception;

public enum MqError {
    UNKNOWN_ERROR(-1),
    SYSTEM_ERROR(-2),
    INTERRUPT(-3),

    METASOURCE_ERROR(-100),

    LOCK_ERROR(-101),
    INVALID_ARG(-102),

    TOPIC_EXIST(-200),
    TOPIC_NOT_EXIST(-201),

    CONSUMER_GROUP_EXIST(-300),
    CONSUMER_GROUP_NOT_EXIST(-301),

    NO_PARTITION_FOR_CONSUMER(-302);

    private int errorCode;

    private MqError(int errorCode) {
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public static MqError convertToMqError(int errorCode) {
        for (MqError e : MqError.values()) {
            if (e.getErrorCode() == errorCode) {
                return e;
            }
        }

        return MqError.UNKNOWN_ERROR;
    }

}
