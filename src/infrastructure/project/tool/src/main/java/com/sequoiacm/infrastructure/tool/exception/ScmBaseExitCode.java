package com.sequoiacm.infrastructure.tool.exception;

public class ScmBaseExitCode {
    public static int SUCCESS = 0;
    // empty std out
    public static int EMPTY_OUT = 1;

    // common sys error >=3 and <60
    public static int INVALID_ARG = 3;
    public static int FILE_NOT_FIND = 4;
    public static int FILE_ALREADY_EXIST = 5;
    public static int PERMISSION_ERROR = 6;
    public static int SHELL_EXEC_ERROR = 7;
    public static int SYSTEM_ERROR = 8;

    // scm error >=60 and <100
    public static int SCM_META_RECORD_ERROR = 60;
    public static int SCM_NOT_EXIST_ERROR = 61;
    public static int SCM_ALREADY_EXIST_ERROR = 62;

    // reserved exit code >=100 and <126

    // sdb error >=170 and <180
    public static int SDB_ERROR = 170;

    // private exit code >=180 and <255

    // max 255
    public static int MAX_VALUE = 255;
}
