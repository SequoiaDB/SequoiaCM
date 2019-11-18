package com.sequoiacm.client.element.privilege;

/**
 * Scm privilege type.
 */
public enum ScmPrivilegeType {
    /**
     * Read privilege.
     */
    READ("READ"),

    /**
     * Create privilege
     */
    CREATE("CREATE"),

    /**
     * Update privilege.
     */
    UPDATE("UPDATE"),

    /**
     * Delete privilege.
     */
    DELETE("DELETE"),

    /**
     * All privilege.
     */
    ALL("ALL"),

    /**
     * Unknown type.
     */
    UNKNOWN("UNKNOWN");

    private String priv;

    private ScmPrivilegeType(String priv) {
        this.priv = priv;
    }

    /**
     * Gets the string format privilege.
     *
     * @return privilege.
     */
    public String getPriv() {
        return priv;
    }

    /**
     * Gets the ScmPrivilegeType with specified string format privilege.
     *
     * @param priv
     *            string format privilege
     * @return ScmPrivilegeType.
     */
    public static ScmPrivilegeType getType(String priv) {
        for (ScmPrivilegeType type : ScmPrivilegeType.values()) {
            if (type.getPriv().equals(priv)) {
                return type;
            }
        }

        return UNKNOWN;
    }
}
