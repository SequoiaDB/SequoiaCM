package com.sequoiacm.mq.tools.exception;

public class ScmExitCode {
    public static int SUCCESS = 0;

    // empty std out
    public static int EMPTY_OUT = 1;

    // common sys error >=3
    public static int COMMON_UNKNOW_ERROR = 3;
    public static int INVALID_ARG = 4;
    public static int FILE_NOT_FIND = 5;
    public static int IO_ERROR = 6;
    public static int PERMISSION_ERROR = 7;
    public static int CONVERT_ERROR = 8;
    public static int SYSTEM_ERROR = 9;
    public static int SHELL_EXEC_ERROR = 10;
    public static int INTERRUPT_ERROR = 11;
    public static int FILE_ALREADY_EXIST = 12;
    public static int UNDEFINE_ERROR = 13;
    public static int UNSUPORT_PLATFORM = 14;
    public static int PARSE_ERROR = 15;
    public static int JPS_WITH_UNAVA_PROCESS = 16;

    // scm error >=20;
    public static int SCM_LOGIN_ERROR = 20;
    public static int SCM_RELOADBIZCONF_ERROR = 21;
    public static int SCM_META_RECORD_ERROR = 22;
    public static int SCM_DUPLICATE_SITE = 23;
    public static int SCM_DUPLICATE_USER = 24;
    public static int SCM_META_CL_MISSING = 25;
    public static int SCM_SERVER_NOT_EXIST = 26;
    public static int SCM_SITE_NOT_EXIST = 27;
    public static int SCM_META_CS_ALREADY_EXIST = 28;
    public static int SCM_META_CS_MISSING = 29;
    public static int SCM_DOMAIN_NOT_EXIST = 30;
    public static int SCM_WORKSPACE_NOT_EXIST = 31;
    public static int SCM_DUPLICATE_SERVER = 32;
    public static int SCM_PORT_OCCUPIED = 33;
    public static int SCM_DUPLICATE_ERROR = 34;
    public static int SCM_NOT_COORD = 35;
    public static int SCM_DUPLICATE_WS = 36;
    public static int SCM_GETPROPERTY_MISSING_MAIN_SITE = 37;
    public static int SCM_GETPROPERTY_FAILED = 38;
    public static int SCM_RELOADCONF_HAVE_FAILED_HOST = 39;
    // sdb error >=70
    public static int SDB_CONNECT_ERROR = 70;
    public static int SDB_QUERY_ERROR = 71;
    public static int SDB_INSERT_ERROR = 72;
    public static int SDB_CREATE_CL_ERROR = 73;
    public static int SDB_CREATE_IDX_ERROR = 74;
    public static int SDB_CREATE_CS_ERROR = 75;
    public static int SDB_UPDATE_ERROR = 76;
    public static int SDB_DELETE_ERROR = 77;
    public static int SDB_SET_DB_ATTR_ERROR = 78;
    public static int SDB_LIST_DOMAINS_ERROR = 79;
    public static int SDB_ATTACH_ERROR = 80;
    public static int SDB_LIST_CS_ERROR = 81;
    public static int SDB_GET_LIST = 82;

    public static int TOPIC_EXIST = 83;
    public static int GROUP_EXIST = 84;
    public static int GROUP_NOT_EXIST = 85;
    public static int TOPIC_NOT_EXIST = 86;
    // max 255
    public static int MAX_VALUE = 255;

}
