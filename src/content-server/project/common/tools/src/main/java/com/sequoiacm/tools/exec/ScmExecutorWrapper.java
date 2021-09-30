package com.sequoiacm.tools.exec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.PropertiesDefine;
import com.sequoiacm.tools.common.PropertiesUtil;
import com.sequoiacm.tools.common.ScmContentCommon;
import com.sequoiacm.tools.element.ScmNodeStatus;
import com.sequoiacm.tools.element.ScmSdbInfo;
import com.sequoiacm.tools.element.ServerInfo;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmExecutorWrapper {
    private ScmExecutor executor;
    private Map<Integer, String> node2Conf;
    private List<ServerInfo> serverList = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(ScmExecutorWrapper.class);

    public ScmExecutorWrapper() throws ScmToolsException {
        if (ScmContentCommon.isLinux()) {
            executor = new ScmLinuxExecutorImpl();
        }
        else {
            throw new ScmToolsException("Unsupport platform", ScmExitCode.UNSUPORT_PLATFORM);
        }
    }

    public void startNode(int port) throws ScmToolsException {
        String nodeConfPath = getNodeConfPath(port);
        try {
            String springConfigLocation = nodeConfPath + File.separator
                    + ScmContentCommon.APPLICATION_PROPERTIES;
            String loggingConfig = nodeConfPath + File.separator + ScmContentCommon.LOGCONF_NAME;

            String logPath = ".." + File.separator + "log" + File.separator
                    + ScmContentCommon.SCM_LOG_DIR_NAME + File.separator + port;
            String errorLogPath = logPath + File.separator + ScmContentCommon.ERROR_LOG_FILE_NAME;
            ScmContentCommon.createDir(logPath);

            Properties sysPro = PropertiesUtil.loadProperties(
                    nodeConfPath + File.separator + ScmContentCommon.APPLICATION_PROPERTIES);
            String options = sysPro.getProperty(PropertiesDefine.PROPERTY_JVM_OPTIONS, "");

            executor.startNode(springConfigLocation, loggingConfig, errorLogPath, options);
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException(
                    "Failed:sequoiacm(" + port + ") failed to start:" + e.getMessage(),
                    e.getExitCode());
        }
    }

    public void stopNode(int port, boolean isForce) throws ScmToolsException {
        int pid = getNodePid(port);
        if (pid < 0) {
            return;
        }
        try {
            executor.stopNode(pid, isForce);
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException("Failed to stop " + port + " node:" + e.getMessage(),
                    e.getExitCode());
        }
    }

    public ScmSdbInfo getMainSiteSdb() {
        scanConfDir();
        // get mainSiteSdb from local conf
        Collection<String> localConfs = node2Conf.values();

        for (String conf : localConfs) {
            if (!conf.endsWith(File.separator)) {
                conf += File.separator;
            }
            try {
                Properties prop = PropertiesUtil
                        .loadProperties(conf + ScmContentCommon.APPLICATION_PROPERTIES);
                String sdb = prop.getProperty(PropertiesDefine.PROPERTY_ROOTSITE_URL);
                if (sdb != null && sdb.length() != 0) {
                    String sdbUser = prop.getProperty(PropertiesDefine.PROPERTY_ROOTSITE_USER, "");
                    String passwdFile = prop.getProperty(PropertiesDefine.PROPERTY_ROOTSITE_PASSWD,
                            "");
                    return new ScmSdbInfo(sdb, sdbUser, passwdFile);
                }
                logger.warn("server's conf file have no root site url:" + conf
                        + ScmContentCommon.APPLICATION_PROPERTIES);
                continue;
            }
            catch (ScmToolsException e) {
                logger.warn("failed to analyze server's conf file:" + conf
                        + ScmContentCommon.APPLICATION_PROPERTIES, e);
                continue;
            }
            catch (Exception e) {
                logger.warn("failed to analyze server's conf file:" + conf
                        + ScmContentCommon.APPLICATION_PROPERTIES, e);
                continue;
            }
        }

        return null;
    }

    public int getNodePid(int port) throws ScmToolsException {
        String confOfPort = getNodeConfPath(port);
        Map<String, Integer> pid2Conf;
        try {
            ScmNodeStatus psRes = executor.getNodeStatus();
            pid2Conf = psRes.getStatusMap();
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException(
                    "Failed to check " + port + " node status:" + e.getMessage(), e.getExitCode());
        }
        if (pid2Conf.containsKey(confOfPort)) {
            return pid2Conf.get(confOfPort);
        }
        else {
            return -1;
        }
    }

    public Map<String, Integer> getNodeStatus() throws ScmToolsException {
        ScmNodeStatus psRes = executor.getNodeStatus();
        return psRes.getStatusMap();
    }

    public String getNodeConfPath(int port) throws ScmToolsException {
        scanConfDir();
        String confOfPort = node2Conf.get(port);
        if (confOfPort == null) {
            throw new ScmToolsException("Can't find conf path of " + port + " node",
                    ScmExitCode.FILE_NOT_FIND);
        }
        return confOfPort;
    }

    public Map<Integer, String> getAllNode() {
        scanConfDir();
        return node2Conf;
    }

    private void scanConfDir() {
        if (node2Conf != null) {
            return;
        }
        node2Conf = new HashMap<>();
        String confPath = ScmContentCommon.getScmConfAbsolutePath();
        File confFile = new File(confPath);
        if (!confFile.isDirectory()) {
            return;
        }
        File[] files = confFile.listFiles();
        for (File f : files) {
            if (f.isDirectory() && !f.getName().equals("samples")) {
                String sysconfPath = confPath + f.getName() + File.separator
                        + ScmContentCommon.APPLICATION_PROPERTIES;
                String confPort;
                try {
                    Properties prop = PropertiesUtil.loadProperties(sysconfPath);
                    confPort = prop.getProperty(PropertiesDefine.PROPERTY_SERVER_PORT);
                }
                catch (ScmToolsException e1) {
                    logger.warn("scan conf dir have some incomplete node's conf file", e1);
                    continue;
                }
                if (confPort != null && !confPort.equals("")) {
                    try {
                        int port = Integer.valueOf(confPort);
                        node2Conf.put(port, confPath + f.getName());
                        ServerInfo info = new ServerInfo(-1, port, confPath + f.getName());
                        serverList.add(info);
                    }
                    catch (Exception e) {
                        logger.warn(
                                "scan conf dir have some incomplete node's conf file:failed to analyze server.port in conf file:"
                                        + sysconfPath + ",server.port:" + confPort);
                    }
                }
                else {
                    logger.warn(
                            "scan conf dir have some incomplete node's conf file:Can't find server.port in conf file:"
                                    + sysconfPath);
                }
            }
        }
    }

    public void addMonitorNodeList(Map<Integer, String> needAddMap) {
        String daemonHomePath = null;
        try {
            // 如果找不到守护进程工具的路径，说明该节点目录与工具不在同一级目录下，且没有将该节点信息添加到监控表，那么就不需要监控
            daemonHomePath = findDaemonHomePath();
        }
        catch (ScmToolsException e) {
            logger.warn(e.getMessage(), e);
            return;
        }

        String scmdScriptPath = daemonHomePath + File.separator + ScmContentCommon.BIN
                + File.separator + ScmContentCommon.DAEMON_SCRIPT;
        for (Map.Entry<Integer, String> entry : needAddMap.entrySet()) {
            int port = entry.getKey();
            String propertiesPath = entry.getValue() + File.separator
                    + ScmContentCommon.APPLICATION_PROPERTIES;
            String shell = scmdScriptPath + " add -o -s on -t SEQUOIACM -c " + propertiesPath;
            try {
                logger.info(
                        "Adding node to monitor table by exec cmd (/bin/sh -c \" " + shell + "\")");
                executor.execShell(shell);
            }
            catch (ScmToolsException e) {
                logger.error("Failed to add node to monitor table,port:{},conf:{}", port,
                        propertiesPath);
                System.out.println("Failed to add node to monitor table,port:" + port + ",conf:"
                        + propertiesPath);
            }
        }
    }

    public void changeMonitorStatus(Set<Integer> needChangeSet, String changeStatus)
            throws ScmToolsException {
        String daemonHomePath = null;
        try {
            daemonHomePath = findDaemonHomePath();
        }
        catch (ScmToolsException e) {
            logger.warn(e.getMessage(), e);
            return;
        }

        String scmdScriptPath = daemonHomePath + File.separator + ScmContentCommon.BIN
                + File.separator + ScmContentCommon.DAEMON_SCRIPT;
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

    private String findDaemonHomePath() throws ScmToolsException {
        String daemonHomePath = ScmContentCommon.DAEMON_DIR_PATH;
        File file = new File(daemonHomePath);
        if (!file.exists()) {
            String daemonLocationFilePath = ScmContentCommon.DAEMON_CONF_FILE_PATH;
            Properties properties = null;
            try {
                properties = PropertiesUtil.loadProperties(daemonLocationFilePath);
                daemonHomePath = properties.getProperty(ScmContentCommon.DAEMON_LOCATION);
            }
            catch (ScmToolsException e) {
                throw new ScmToolsException(
                        "Failed to load properties,properties:" + file.getAbsolutePath(),
                        e.getExitCode(), e);
            }
            if (daemonHomePath == null) {
                throw new ScmToolsException(
                        "Invalid args:" + ScmContentCommon.DAEMON_LOCATION + " is null",
                        ScmExitCode.INVALID_ARG);
            }
        }
        return daemonHomePath;
    }
}
