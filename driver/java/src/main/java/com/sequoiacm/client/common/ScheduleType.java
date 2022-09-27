package com.sequoiacm.client.common;

/**
 * Scm schedule type.
 */
public enum ScheduleType {
    /**
     * Copy file.
     */
    COPY_FILE("copy_file"),

    /**
     * Clean file.
     */
    CLEAN_FILE("clean_file"),

    /**
     * Move file.
     */
    MOVE_FILE("move_file"),

    /**
     * Recycle space.
     */
    RECYCLE_SPACE("recycle_space"),

    /**
     * Unknown type.
     */
    UNKOWN_SCHEDULE("unknown");

    private String name;

    private ScheduleType(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the type.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets type with specified name.
     *
     * @param name
     *            type name.
     * @return ScheduleType.
     */
    public static ScheduleType getType(String name) {
        for (ScheduleType type : ScheduleType.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }

        return UNKOWN_SCHEDULE;
    }
}
