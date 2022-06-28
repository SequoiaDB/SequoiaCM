package com.sequoiacm.config.tools.common;

public enum ConfigType {
    BY_SERVICE(0),
    BY_NODE(1);

    private final Integer type;

    ConfigType(Integer type) {
        this.type = type;
    }

    public Integer getType() {
        return type;
    }
}
