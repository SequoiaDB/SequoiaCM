package com.sequoiacm.diagnose.common;

public enum CheckLevel {
    SIZE(1),
    MD5(2),
    UNKNOWN(Integer.MAX_VALUE);

    CheckLevel(int value) {
        this.value = value;
    }

    private int value;

    public int getValue() {
        return value;
    }

    public static CheckLevel getType(int value) {
        for (CheckLevel type : CheckLevel.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
