package com.sequoiacm.daemon.common;

import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.io.File;

public class ArgsUtils {

    public static void checkPortValid(int port) throws ScmToolsException {
        if (port <= 0 || port > 65535) {
            throw new ScmToolsException("Invalid args: -p or --port{" + port + "} is out of range",
                    ScmExitCode.INVALID_ARG);
        }
    }

    public static void checkStatusValid(String status) throws ScmToolsException {
        if (!status.equals(DaemonDefine.NODE_STATUS_ON)
                && !status.equals(DaemonDefine.NODE_STATUS_OFF)) {
            throw new ScmToolsException("Invalid args: please set -s or --status "
                    + DaemonDefine.NODE_STATUS_ON + " or " + DaemonDefine.NODE_STATUS_OFF,
                    ScmExitCode.INVALID_ARG);
        }
    }

    public static void checkTypeValid(ScmServerScriptEnum serverScriptEnum, String confPath)
            throws ScmToolsException {
        if (serverScriptEnum == null) {
            throw new ScmToolsException("Invalid args: type isn't exist", ScmExitCode.INVALID_ARG);
        }
        if (!confPath.contains(serverScriptEnum.getDirName())
                || !confPath.contains(serverScriptEnum.getType().toLowerCase())) {
            throw new ScmToolsException("Invalid args: type isn't compatible with conf",
                    ScmExitCode.INVALID_ARG);
        }
    }

    public static void checkPathExist(String confPath) throws ScmToolsException {
        File file = new File(confPath);
        if (!file.exists()) {
            throw new ScmToolsException("Invalid args: conf {" + confPath + "} is not exist",
                    ScmExitCode.FILE_NOT_FIND);
        }
    }

    public static String convertPathToAbsolute(String path) {
        File file = new File(path);
        return file.getAbsolutePath();
    }

    public static int convertAndCheckPeriod(String periodStr) throws ScmToolsException {
        int period = 0;
        try {
            period = ScmCommon.convertStrToInt(periodStr);
            if (period <= 0) {
                throw new ScmToolsException("Invalid args: period isn't a positive number",
                        ScmExitCode.INVALID_ARG);
            }
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Invalid args: period convert to int failed",
                    e.getExitCode(), e);
        }
        return period;
    }
}
