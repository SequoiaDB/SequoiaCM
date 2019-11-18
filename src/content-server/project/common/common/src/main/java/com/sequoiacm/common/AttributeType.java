package com.sequoiacm.common;

public enum AttributeType {
    STRING("STRING"),
    INTEGER("INTEGER"),
    DATE("DATE"),
    DOUBLE("DOUBLE"),
    BOOLEAN("BOOLEAN"),
    UNKOWN_TYPE("unknown");

    private String name;

    private AttributeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static AttributeType getType(String name) {
        for (AttributeType type : AttributeType.values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return UNKOWN_TYPE;
    }
}
