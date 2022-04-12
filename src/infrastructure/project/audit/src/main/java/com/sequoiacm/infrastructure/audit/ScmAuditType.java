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

    public static int S3_BUCKET_DML_FLAG       = 0x00010000;
    public static int S3_OBJECT_DQL_FLAG       = 0x00020000;
    public static int S3_OBJECT_DML_FLAG       = 0x00040000;

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

    //monitor
    public static int MONITOR_FLAG             = 0x10000000;

    public static int SCM_BUCKET_DML_FLAG           = 0x40000000;

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

    // USER_DML | ROLE_DML
    //user dml
    USER_DML(ScmAuditTopLevel.USER_DML_FLAG,"USER_DML"),
    CREATE_USER(ScmAuditTopLevel.USER_DML_FLAG ^ 1, "CREATE_USER"),
    DELETE_USER(ScmAuditTopLevel.USER_DML_FLAG ^ 2, "DELETE_USER"),
    UPDATE_USER(ScmAuditTopLevel.USER_DML_FLAG ^ 3, "UPDATE_USER"),

    CREATE_ROLE(ScmAuditTopLevel.USER_DML_FLAG ^ 4, "CREATE_ROLE"),
    DELETE_ROLE(ScmAuditTopLevel.USER_DML_FLAG ^ 5, "DELETE_ROLE"),
    UPDATE_ROLE(ScmAuditTopLevel.USER_DML_FLAG ^ 6, "UPDATE_ROLE"),
    GRANT(ScmAuditTopLevel.USER_DML_FLAG ^ 7, "GRANT"),
    REVOKE(ScmAuditTopLevel.USER_DML_FLAG ^ 8, "REVOKE"),

    //user ( role ) dql
    USER_DQL(ScmAuditTopLevel.USER_DQL_FLAG, "USER_DQL"),

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
    CREATE_META_ATTR(ScmAuditTopLevel.META_CLASS_DML_FLAG ^ 4, "CREATE_META_ATTR"),
    DELETE_META_ATTR(ScmAuditTopLevel.META_CLASS_DML_FLAG ^ 5, "DELETE_META_ATTR"),
    UPDATE_META_ATTR(ScmAuditTopLevel.META_CLASS_DML_FLAG ^ 6, "UPDATE_META_ATTR"),

    //meta_data dql
    META_CLASS_DQL(ScmAuditTopLevel.META_CLASS_DQL_FLAG, "META_CLASS_DQL"),

    // monitor flag
    MONITOR_DELETE_INSTANCE(ScmAuditTopLevel.MONITOR_FLAG ^ 1, "DELETE_INSTANCE"),

    // SCM Bucket
    SCM_BUCKET_DML(ScmAuditTopLevel.SCM_BUCKET_DML_FLAG, "SCM_BUCKET_DML"),
    CREATE_SCM_BUCKET(ScmAuditTopLevel.SCM_BUCKET_DML_FLAG ^ 1, "CREATE_SCM_BUCKET"),
    DELETE_SCM_BUCKET(ScmAuditTopLevel.SCM_BUCKET_DML_FLAG ^ 2, "DELETE_SCM_BUCKET"),
    UPDATE_SCM_BUCKET(ScmAuditTopLevel.SCM_BUCKET_DML_FLAG ^ 3, "UPDATE_SCM_BUCKET"),

    // S3 Bucket
    S3_BUCKET_DML(ScmAuditTopLevel.S3_BUCKET_DML_FLAG, "S3_BUCKET_DML"),
    CREATE_S3_BUCKET(ScmAuditTopLevel.S3_BUCKET_DML_FLAG ^ 1, "CREATE_S3_BUCKET"),
    DELETE_S3_BUCKET(ScmAuditTopLevel.S3_BUCKET_DML_FLAG ^ 2, "DELETE_S3_BUCKET"),
    UPDATE_S3_BUCKET(ScmAuditTopLevel.S3_BUCKET_DML_FLAG ^ 3, "UPDATE_S3_BUCKET"),

    S3_OBJECT_DML(ScmAuditTopLevel.S3_OBJECT_DML_FLAG, "S3_OBJECT_DML"),
    CREATE_S3_OBJECT(ScmAuditTopLevel.S3_OBJECT_DML_FLAG ^ 1, "CREATE_S3_OBJECT"),
    UPDATE_S3_OBJECT(ScmAuditTopLevel.S3_OBJECT_DML_FLAG ^ 2, "UPDATE_S3_OBJECT"),
    DELETE_S3_OBJECT(ScmAuditTopLevel.S3_OBJECT_DML_FLAG ^ 3, "DELETE_S3_OBJECT"),
    S3_OBJECT_DQL(ScmAuditTopLevel.S3_OBJECT_DQL_FLAG, "S3_OBJECT_DQL"),
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
