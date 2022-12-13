package com.sequoiacm.diagnose.execption;

import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class CollectException extends ScmBaseExitCode {
    public static int COPY_FILE_FAILED = 190;
    public static int SSH_CONNECT_FAILED = 191;
    public static int TAR_FILE_FAILED = 192;
    public static int GET_PID_FAILED = 193;

    public static String getExceptionStack(Exception e) throws IOException {
        String stackToString;
        StringWriter stringWriter = null;
        PrintWriter writer = null;
        try {
            stringWriter = new StringWriter();
            writer = new PrintWriter(stringWriter);
            e.printStackTrace(writer);
            stackToString = stringWriter.getBuffer().toString();
        }
        finally {
            if (writer != null) {
                writer.close();
            }
            if (stringWriter != null) {
                stringWriter.close();
            }
        }
        return stackToString;
    }
}
