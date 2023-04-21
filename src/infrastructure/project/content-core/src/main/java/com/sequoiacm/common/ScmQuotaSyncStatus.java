package com.sequoiacm.common;

public enum ScmQuotaSyncStatus {
    SYNCING("syncing"),
    COMPLETED("completed"),
    CANCELED("canceled"),
    FAILED("failed");

    private final String name;

    ScmQuotaSyncStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ScmQuotaSyncStatus getByName(String name) {
        for (ScmQuotaSyncStatus value : ScmQuotaSyncStatus.values()) {
            if (value.getName().equals(name)) {
                return value;
            }
        }
        return null;
    }
}
