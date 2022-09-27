package com.sequoiacm.client.common;

public enum ScmDataCheckLevel {

    WEEK("week"),
    STRICT("strict"),
    UNKNOWN("unknown");

    ScmDataCheckLevel(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return name;
    }

    public static ScmDataCheckLevel getType(String name) {
        for (ScmDataCheckLevel type : ScmDataCheckLevel.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
