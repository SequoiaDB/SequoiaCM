package com.sequoiacm.common;

public enum ScmSiteCacheStrategy {
    ALWAYS,
    NEVER,
    AUTO,
    UNKNOWN;

    public static ScmSiteCacheStrategy getStrategy(String strategyStr) {
        for (ScmSiteCacheStrategy strategy : ScmSiteCacheStrategy.values()) {
            if (strategy.name().equalsIgnoreCase(strategyStr)) {
                return strategy;
            }
        }
        return UNKNOWN;
    }
}
