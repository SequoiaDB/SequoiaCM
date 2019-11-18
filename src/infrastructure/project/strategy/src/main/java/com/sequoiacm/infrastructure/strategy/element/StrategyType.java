package com.sequoiacm.infrastructure.strategy.element;

import com.sequoiacm.infrastructure.strategy.exception.StrategyException;

public enum StrategyType {

    STAR("star", 1), NETWORK("network", 2);

    private String name;
    private int type;

    private StrategyType(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }
    
    public static StrategyType getType(String name) throws StrategyException {
        for (StrategyType type : StrategyType.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        throw new StrategyException("unknown startegy type: " + name);
    }
}
