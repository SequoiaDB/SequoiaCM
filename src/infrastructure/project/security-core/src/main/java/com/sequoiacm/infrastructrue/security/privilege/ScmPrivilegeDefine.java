package com.sequoiacm.infrastructrue.security.privilege;

public enum ScmPrivilegeDefine {
    READ(0x21, "READ"),
    LOW_LEVEL_READ(0x1, "LOW_LEVEL_READ"),
    CREATE(0x2, "CREATE"),
    UPDATE(0x4, "UPDATE"),
    DELETE(0x8, "DELETE"),
    EXECUTE(0x10, "EXECUTE"),
    ALL(0xFFFF, "ALL");

    private int flag;
    private String name;

    private ScmPrivilegeDefine(int flag, String name) {
        this.flag = flag;
        this.name = name;
    }

    public int getFlag() {
        return flag;
    }

    public String getName() {
        return name;
    }

    public static ScmPrivilegeDefine getEnum(String name) {
        for (ScmPrivilegeDefine one : ScmPrivilegeDefine.values()) {
            if (one.getName().equals(name)) {
                return one;
            }
        }

        return null;
    }
}
