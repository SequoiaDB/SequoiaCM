package com.sequoiacm.infrastructure.tool.common;

import com.sequoiacm.infrastructure.tool.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.exec.ScmLinuxExecutorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class ScmMonitorDaemonHelper {
    private static Logger logger = LoggerFactory.getLogger(ScmMonitorDaemonHelper.class);
    private static ScmLinuxExecutorImpl executor = new ScmLinuxExecutorImpl();

    public static void addMonitorNodeList(List<ScmNodeInfo> needAddList, String scmInstallPath) {
        String daemonHomePath = null;
        try {
            // 如果找不到守护进程工具的路径，说明该节点目录与工具不在同一级目录下，且没有将该节点信息添加到监控表，那么就不需要监控
            daemonHomePath = findDaemonHomePath(scmInstallPath);
        }
        catch (ScmToolsException e) {
            logger.warn(e.getMessage(), e);
            return;
        }

        String scmdScriptPath = daemonHomePath + File.separator + ScmCommon.BIN + File.separator
                + ScmCommon.DAEMON_SCRIPT;
        for (ScmNodeInfo node : needAddList) {
            String propPath = node.getConfPath() + File.separator
                    + ScmCommon.APPLICATION_PROPERTIES;
            String shell = scmdScriptPath + " add -o -s on -t " + node.getNodeType().getUpperName()
                    + " -c " + propPath;
            try {
                logger.info(
                        "Adding node to monitor table by exec cmd (/bin/sh -c \" " + shell + "\")");
                executor.execShell(shell);
            }
            catch (ScmToolsException e) {
                logger.error("Failed to add node to monitor table,port:{},conf:{}", node.getPort(),
                        propPath, e);
                System.out.println("Failed to add node to monitor table,port:" + node.getPort()
                        + ",conf:" + propPath + " error:" + e.getMessage());
            }
        }
    }

    public static void changeMonitorStatus(Set<Integer> needChangeSet, String changeStatus,
            String scmInstallPath) throws ScmToolsException {
        String daemonHomePath = null;
        try {
            daemonHomePath = findDaemonHomePath(scmInstallPath);
        }
        catch (ScmToolsException e) {
            if (e.getExitCode() == ScmBaseExitCode.FILE_NOT_FIND) {
                logger.warn(e.getMessage(), e);
                return;
            }
            throw e;
        }

        String scmdScriptPath = daemonHomePath + File.separator + ScmCommon.BIN + File.separator
                + ScmCommon.DAEMON_SCRIPT;
        for (Integer port : needChangeSet) {
            String shell = scmdScriptPath + " chstatus -p " + port + " -s " + changeStatus;
            try {
                logger.info("Changing node status by exec cmd (/bin/sh -c \" " + shell + "\")");
                executor.execShell(shell);
            }
            catch (ScmToolsException e) {
                throw new ScmToolsException("Failed to change node monitor status,port:" + port
                        + ", status:" + changeStatus, e.getExitCode());
            }
        }
    }

    private static String findDaemonHomePath(String scmInstallPath) throws ScmToolsException {
        if (!scmInstallPath.endsWith(File.separator)) {
            scmInstallPath = scmInstallPath + File.separator;
        }
        String daemonHomePath = scmInstallPath + "daemon";
        File file = new File(daemonHomePath);
        if (!file.exists()) {
            String daemonLocationFilePath = ScmCommon.DAEMON_CONF_FILE_PATH;
            try {
                Properties properties = PropertiesUtil.loadProperties(daemonLocationFilePath);
                daemonHomePath = properties.getProperty(ScmCommon.DAEMON_LOCATION);
            }
            catch (ScmToolsException e) {
                throw new ScmToolsException(
                        "Failed to load properties,properties:" + daemonLocationFilePath,
                        e.getExitCode(), e);
            }
            if (daemonHomePath == null || daemonHomePath.length() == 0) {
                throw new ScmToolsException(
                        "Invalid args:" + ScmCommon.DAEMON_LOCATION + " is null",
                        ScmBaseExitCode.INVALID_ARG);
            }
            file = new File(daemonHomePath);
            if (!file.exists()) {
                throw new ScmToolsException(
                        "Failed to find daemon home path, caused by daemon home dir not exist, dir:"
                                + daemonHomePath,
                        ScmBaseExitCode.FILE_NOT_FIND);
            }
        }
        return daemonHomePath;
    }
}
