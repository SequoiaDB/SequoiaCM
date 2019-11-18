package com.sequoiacm.infrastructure.audit;

class ScmUserAuditTopLevel {

    // ldap_user
    public static int LDAP_USER_FLAG = 0x00000100;

    // local_user
    public static int LOCAL_USER_FLAG = 0x00000200;

    // token_user
    public static int TOKEN_USER_FLAG = 0x00000400;

    // system_user
    public static int SYSTEM_USER_FLAG = 0x00000800;

    // all_user
    public static int ALL_USER_FLAG = 0xFFFFFF00;

}

public enum ScmUserAuditType {

    LDAP_USER(ScmUserAuditTopLevel.LDAP_USER_FLAG, "LDAP"),
    LOCAL_USER(ScmUserAuditTopLevel.LOCAL_USER_FLAG, "LOCAL"),
    TOKEN_USER(ScmUserAuditTopLevel.TOKEN_USER_FLAG,"TOKEN"),
    SYSTEM_USER(ScmUserAuditTopLevel.SYSTEM_USER_FLAG, "SYSTEM"),
    ALL_USER(ScmUserAuditTopLevel.ALL_USER_FLAG, "ALL");

    private boolean isTopLevel = false;
    private int type;
    private String name;

    private ScmUserAuditType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public boolean isTopLevel() {
        return isTopLevel;
    }

    public static ScmUserAuditType getScmUserAuditType(String name) {
        for (ScmUserAuditType value : ScmUserAuditType.values()) {
            if (name.equals(value.getName())) {
                return value;
            }
        }

        return null;
    }
}
