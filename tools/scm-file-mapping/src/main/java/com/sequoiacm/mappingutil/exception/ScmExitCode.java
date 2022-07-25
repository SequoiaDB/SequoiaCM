package com.sequoiacm.mappingutil.exception;

import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;

public class ScmExitCode extends ScmBaseExitCode {

    // private exit code >=180 and <255
    public static int TOO_MANY_FAILURES = 180;

    public static int WORK_CONF_ERROR = 190;

    public static int WORK_PATH_CONFLICT = 220;

    public static int BAD_CREDENTIAL = 240;
}