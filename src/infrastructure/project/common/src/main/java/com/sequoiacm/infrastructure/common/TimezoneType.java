package com.sequoiacm.infrastructure.common;

public enum TimezoneType {
    ASIA_SHANGHAI(1, "Asia/Shanghai"),
    NULL(0, null),
    UNKNOWN(-1, "unknown");

    private int timezoneId;
    private String timezoneName;

    TimezoneType(int timezoneId, String timezoneName) {
        this.timezoneId = timezoneId;
        this.timezoneName = timezoneName;
    }

    public int getTimezoneId() {
        return timezoneId;
    }

    public String getTimezoneName() {
        return timezoneName;
    }

    public static TimezoneType getType(int timezoneId) {
        for (TimezoneType type : TimezoneType.values()) {
            if (type.getTimezoneId() == timezoneId) {
                return type;
            }
        }
        throw new IllegalArgumentException("invalid timezoneId:" + timezoneId);
    }
}
