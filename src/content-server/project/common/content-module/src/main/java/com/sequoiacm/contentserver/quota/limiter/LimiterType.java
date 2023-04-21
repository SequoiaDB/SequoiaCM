package com.sequoiacm.contentserver.quota.limiter;

public enum LimiterType {
    STABLE("stable"),
    SYNCING("syncing"),
    UNLIMITED("unlimited"),
    NONE("none");

    private String name;

    LimiterType(String name) {
        this.name = name;
    }
}
