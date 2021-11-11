package com.sequoiacm.daemon.common;

import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.io.File;

public class ArgsUtils {

    public static int convertAndCheckPortValid(String portStr) throws ScmToolsException {
        int port = 0;
        try {
            port = ScmCommon.convertStrToInt(portStr);
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Invalid args: port convert to number(int) failed, port=" + portStr,
                    e.getExitCode(), e);
        }
        if (port <= 0 || port > 65535) {
            throw new ScmToolsException(
                    "Invalid args: -p or --port is out of range, port preferred in the range (0,65535], port="
                            + port,
                    ScmExitCode.INVALID_ARG);
        }
        return port;
    }

    public static void checkStatusValid(String status) throws ScmToolsException {
        if (!status.equals(DaemonDefine.NODE_STATUS_ON)
                && !status.equals(DaemonDefine.NODE_STATUS_OFF)) {
            throw new ScmToolsException(
                    "Invalid args: -s or --status is wrong option, please set -s or --status "
                            + DaemonDefine.NODE_STATUS_ON + " or " + DaemonDefine.NODE_STATUS_OFF
                            + " ,status=" + status,
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
            throw new ScmToolsException(
                    "Invalid args: type isn't compatible with conf, type="
                            + serverScriptEnum.getType() + " ,confPath=" + confPath,
                    ScmExitCode.INVALID_ARG);
        }
    }

    public static void checkPathExist(String confPath) throws ScmToolsException {
        File file = new File(confPath);
        if (!file.exists()) {
            throw new ScmToolsException("Invalid args: conf is not exist, conf=" + confPath,
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
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Invalid args: period convert to number(int) failed, period=" + periodStr,
                    e.getExitCode(), e);
        }
        if (period <= 0 || period >= DaemonDefine.PERIOD_MAXIMUM) {
            throw new ScmToolsException(
                    "Invalid args: period is out of range, period preferred in the range (0,"
                            + DaemonDefine.PERIOD_MAXIMUM + "), period=" + period,
                    ScmExitCode.INVALID_ARG);
        }
        return period;
    }
}
