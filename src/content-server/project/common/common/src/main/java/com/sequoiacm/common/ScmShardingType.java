package com.sequoiacm.common;

public enum ScmShardingType {
    NONE("none", 0), MONTH("month", 1), YEAR("year", 2), QUARTER("quarter", 3);

    private String name;
    private int type;

    private ScmShardingType(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public static ScmShardingType getShardingType(String type) {
        for(ScmShardingType value:ScmShardingType.values()) {
            if(value.getName().equals(type)) {
                return value;
            }
        }
        return null;
    }
}