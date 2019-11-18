package com.sequoiacm.deploy.module;

public enum SiteStrategyType {
    NETWORK("network"),
    STAR("star");
    private String type;

    private SiteStrategyType(String typeStr) {
        this.type = typeStr;
    }

    public String getType() {
        return type;
    }

    public static SiteStrategyType getEnumByString(String typeStr) {
        for (SiteStrategyType type : SiteStrategyType.values()) {
            if (typeStr.equalsIgnoreCase(type.getType())) {
                return type;
            }
        }

        throw new IllegalArgumentException("no such site strategy type:" + typeStr);
    }
}
