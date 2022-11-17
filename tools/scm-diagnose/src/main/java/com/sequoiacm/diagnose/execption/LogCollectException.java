package com.sequoiacm.diagnose.execption;

import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;

public class LogCollectException extends ScmBaseExitCode {
    public static int COPY_FILE_FAILED = 190;
    public static int SSH_CONNECT_FAILED = 191;
    public static int TAR_FILE_FAILED = 192;
}
