package com.sequoiacm.infrastructure.audit;

class ScmAuditTopLevel {
    //all support level is 6 * 4 = 24(ALL_FLAG=0xFFFFFF00)
    //each level have 2^8 -1 = 255

    //file_dml(create/delete/update)
    public static int FILE_DML_FLAG            = 0x00000100;

    //file_dql(read)
    public static int FILE_DQL_FLAG            = 0x00000200;

    //ws_dml(create/delete/update)
    public static int WS_DML_FLAG              = 0x00000400;

    //ws_dql
    public static int WS_DQL_FLAG              = 0x00000800;

    //dir_dml(create/delete/update)
    public static int DIR_DML_FLAG             = 0x00001000;

    //dir_dql
    public static int DIR_DQL_FLAG             = 0x00002000;

    //user_dml(create/delete/update)
    public static int USER_DML_FLAG            = 0x00004000;

    //user_dql
    public static int USER_DQL_FLAG            = 0x00008000;

    //role_dml(create/delete/update)
    public static int ROLE_DML_FLAG            = 0x00010000;

    //role_dql
    public static int ROLE_DQL_FLAG            = 0x00020000;

    //grant
    public static int GRANT_FLAG               = 0x00040000;

    //login
    public static int LOGIN_FLAG               = 0x00080000;

    //schedule_dml(create/delete/update)
    public static int SCHEDULE_DML_FLAG        = 0x00100000;

    //schedule_dql
    public static int SCHEDULE_DQL_FLAG        = 0x00200000;

    //batch_dml(create/delete/update)
    public static int BATCH_DML_FLAG           = 0x00400000;

    //batch_dql
    public static int BATCH_DQL_FLAG           = 0x00800000;

    //meta_class_dml
    public static int META_CLASS_DML_FLAG      = 0x01000000;

    //meta_class_dql
    public static int META_CLASS_DQL_FLAG      = 0x02000000;

    //meta_attr_dml
    public static int META_ATTR_DML_FLAG       = 0x04000000;

    //meta_attr_dql
    public static int META_ATTR_DQL_FLAG       = 0x08000000;

    public static int ALL_FLAG                 = 0xFFFFFF00;
}

public enum ScmAuditType {
    //file dml
    FILE_DML(ScmAuditTopLevel.FILE_DML_FLAG, "FILE_DML"),
    CREATE_FILE(ScmAuditTopLevel.FILE_DML_FLAG ^ 1, "CREATE_FILE"),
    DELETE_FILE(ScmAuditTopLevel.FILE_DML_FLAG ^ 2, "DELETE_FILE"),
    UPDATE_FILE(ScmAuditTopLevel.FILE_DML_FLAG ^ 3, "UPDATE_FILE"),

    //file dql
    FILE_DQL(ScmAuditTopLevel.FILE_DQL_FLAG,"FILE_DQL"),

    //workspace dml
    WS_DML(ScmAuditTopLevel.WS_DML_FLAG,"WS_DML"),
    CREATE_WS(ScmAuditTopLevel.WS_DML_FLAG ^ 1, "CREATE_WS"),
    DELETE_WS(ScmAuditTopLevel.WS_DML_FLAG ^ 2, "DELETE_WS"),
    UPDATE_WS(ScmAuditTopLevel.WS_DML_FLAG ^ 3, "UPDATE_WS"),

    //workspace dql
    WS_DQL(ScmAuditTopLevel.WS_DQL_FLAG,"WS_DQL"),

    //dir dml
    DIR_DML(ScmAuditTopLevel.DIR_DML_FLAG,"DIR_DML"),
    CREATE_DIR(ScmAuditTopLevel.DIR_DML_FLAG ^ 1, "CREATE_DIR"),
    DELETE_DIR(ScmAuditTopLevel.DIR_DML_FLAG ^ 2, "DELETE_DIR"),
    UPDATE_DIR(ScmAuditTopLevel.DIR_DML_FLAG ^ 3, "UPDATE_DIR"),

    //dir dql
    DIR_DQL(ScmAuditTopLevel.DIR_DQL_FLAG,"DIR_DQL"),

    //user dml
    USER_DML(ScmAuditTopLevel.USER_DML_FLAG,"USER_DML"),
    CREATE_USER(ScmAuditTopLevel.USER_DML_FLAG ^ 1, "CREATE_USER"),
    DELETE_USER(ScmAuditTopLevel.USER_DML_FLAG ^ 2, "DELETE_USER"),
    UPDATE_USER(ScmAuditTopLevel.USER_DML_FLAG ^ 3, "UPDATE_USER"),

    //user dql
    USER_DQL(ScmAuditTopLevel.USER_DQL_FLAG, "USER_DQL"),

    //role dml
    ROLE_DML(ScmAuditTopLevel.ROLE_DML_FLAG,"ROLE_DML"),
    CREATE_ROLE(ScmAuditTopLevel.ROLE_DML_FLAG ^ 1, "CREATE_ROLE"),
    DELETE_ROLE(ScmAuditTopLevel.ROLE_DML_FLAG ^ 2, "DELETE_ROLE"),
    UPDATE_ROLE(ScmAuditTopLevel.ROLE_DML_FLAG ^ 3, "UPDATE_ROLE"),

    //role dql
    ROLE_DQL(ScmAuditTopLevel.ROLE_DQL_FLAG, "ROLE_DQL"),

    //GRANT_FLAG
    GRANT(ScmAuditTopLevel.GRANT_FLAG, "GRANT"),
    REVOKE(ScmAuditTopLevel.GRANT_FLAG ^ 1, "REVOKE"),
    //login
    LOGIN(ScmAuditTopLevel.LOGIN_FLAG,"LOGIN"),
    LOGOUT(ScmAuditTopLevel.LOGIN_FLAG ^ 1,"LOGOUT"),

    //schedule dml
    SCHEDULE_DML(ScmAuditTopLevel.SCHEDULE_DML_FLAG, "SCHEDULE_DML"),
    CREATE_SCHEDULE(ScmAuditTopLevel.SCHEDULE_DML_FLAG ^ 1, "CREATE_SCHEDULE"),
    DELETE_SCHEDULE(ScmAuditTopLevel.SCHEDULE_DML_FLAG ^ 2, "DELETE_SCHEDULE"),
    UPDATE_SCHEDULE(ScmAuditTopLevel.SCHEDULE_DML_FLAG ^ 3, "UPDATE_SCHEDULE"),

    //schedule dml
    SCHEDULE_DQL(ScmAuditTopLevel.SCHEDULE_DQL_FLAG, "SCHEDULE_DQL"),

    //batch dml
    BATCH_DML(ScmAuditTopLevel.BATCH_DML_FLAG, "BATCH_DML"),
    CREATE_BATCH(ScmAuditTopLevel.BATCH_DML_FLAG ^ 1, "CREATE_BATCH"),
    DELETE_BATCH(ScmAuditTopLevel.BATCH_DML_FLAG ^ 2, "DELETE_BATCH"),
    UPDATE_BATCH(ScmAuditTopLevel.BATCH_DML_FLAG ^ 3, "UPDATE_BATCH"),

    //batch dql
    BATCH_DQL(ScmAuditTopLevel.BATCH_DQL_FLAG, "BATCH_DQL"),

    //meta_data dml
    META_CLASS_DML(ScmAuditTopLevel.META_CLASS_DML_FLAG, "META_CLASS_DML"),
    CREATE_META_CLASS(ScmAuditTopLevel.META_CLASS_DML_FLAG ^ 1, "CREATE_META_CLASS"),
    DELETE_META_CLASS(ScmAuditTopLevel.META_CLASS_DML_FLAG ^ 2, "DELETE_META_CLASS"),
    UPDATE_META_CLASS(ScmAuditTopLevel.META_CLASS_DML_FLAG ^ 3, "UPDATE_META_CLASS"),

    //meta_data dql
    META_CLASS_DQL(ScmAuditTopLevel.META_CLASS_DQL_FLAG, "META_CLASS_DQL"),

  //meta_data dml
    META_ATTR_DML(ScmAuditTopLevel.META_ATTR_DML_FLAG, "META_ATTR_DML"),
    CREATE_META_ATTR(ScmAuditTopLevel.META_ATTR_DML_FLAG ^ 1, "CREATE_META_ATTR"),
    DELETE_META_ATTR(ScmAuditTopLevel.META_ATTR_DML_FLAG ^ 2, "DELETE_META_ATTR"),
    UPDATE_META_ATTR(ScmAuditTopLevel.META_ATTR_DML_FLAG ^ 3, "UPDATE_META_ATTR"),

    //meta_data dql
    META_ATTR_DQL(ScmAuditTopLevel.META_ATTR_DQL_FLAG, "META_ATTR_DQL"),

    //all
    ALL(ScmAuditTopLevel.ALL_FLAG,"ALL");


    private boolean isTopLevel = false;
    private int type;
    private String name;
    private ScmAuditType(int type, String name) {
        if ((type & ScmAuditTopLevel.ALL_FLAG) == type) {
            this.isTopLevel = true;
        }
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

    public static ScmAuditType getScmAuditType(String name) {
        for (ScmAuditType value : ScmAuditType.values()) {
            if (name.equals(value.getName())) {
                return value;
            }
        }

        return null;
    }

}
