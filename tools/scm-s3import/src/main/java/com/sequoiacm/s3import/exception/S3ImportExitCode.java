package com.sequoiacm.s3import.exception;

import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;

public class S3ImportExitCode extends ScmBaseExitCode {

    // private exit code >=180 and <255
    public static int EXEC_TIME_OUT = 190;
    public static int TOO_MANY_FAILURE = 191;

    public static int ETAG_NOT_MATCH = 210;

    public static int ENV_ERROR = 220;
    public static int WORK_PATH_CONFLICT = 221;
}
