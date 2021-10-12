package com.sequoiacm.infrastructure.tool.exec;

import com.sequoiacm.infrastructure.tool.common.PropertiesUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.*;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class ScmExecutorWrapper {
    private ScmExecutor executor;
    private Map<Integer, ScmNodeInfo> node2Conf;
    private Map<ScmNodeType, Map<Integer, ScmNodeInfo>> nodeType2Node;
    private static final Logger logger = LoggerFactory.getLogger(ScmExecutorWrapper.class);
    private ScmNodeTypeList allNodeType;

    public ScmExecutorWrapper(ScmNodeTypeList allNodeType) throws ScmToolsException {
        this.allNodeType = allNodeType;
        if (ScmCommon.isLinux()) {
            executor = new ScmLinuxExecutorImpl(allNodeType);
        }
        else {
            // TODO:throw new ScmToolsException("Unsupport platform",
            // ScmExitCode.UNSUPORT_PLATFORM);
        }
    }

    public void startNode(ScmNodeInfo node) throws ScmToolsException {
        String nodeConfPath = node.getConfPath();
        try {
            String springConfigLocation = nodeConfPath + File.separator
                    + ScmToolsDefine.FILE_NAME.APP_PROPS;
            String loggingConfig = nodeConfPath + File.separator + ScmToolsDefine.FILE_NAME.LOGBACK;

            String jarName = ScmHelper.getJarNameByType(node.getNodeType());
            String jarPath = ScmHelper.getPwd() + File.separator + ScmToolsDefine.FILE_NAME.JARS
                    + File.separator + jarName;

            String logPath = "." + File.separator + ScmToolsDefine.FILE_NAME.LOG + File.separator
                    + node.getNodeType().getName() + File.separator + node.getPort();
            String errorLogPath = logPath + File.separator + ScmToolsDefine.FILE_NAME.ERROR_OUT;
            ScmHelper.createDir(logPath);

            Properties sysPro = PropertiesUtil.loadProperties(springConfigLocation);
            String options = sysPro.getProperty(ScmToolsDefine.SCM_JVM_OPTIONS, "");

            executor.startNode(jarPath, springConfigLocation, loggingConfig, errorLogPath, options);
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException(
                    "Failed:sequoiacm(" + node.getPort() + ") failed to start:" + e.getMessage(),
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

    public int getNodePid(int port) throws ScmToolsException {
        String confOfPort = getNode(port).getConfPath();
        Map<String, ScmNodeProcessInfo> confNodeProcess;
        try {
            ScmNodeStatus psRes = executor.getNodeStatus();
            confNodeProcess = psRes.getStatusMap();
        }
        catch (ScmToolsException e) {
            throw new ScmToolsException(
                    "Failed to check " + port + " node status:" + e.getMessage(), e.getExitCode());
        }
        if (confNodeProcess.containsKey(confOfPort)) {
            return confNodeProcess.get(confOfPort).getPid();
        }
        else {
            return -1;
        }
    }

    public Map<String, ScmNodeProcessInfo> getNodeStatus() throws ScmToolsException {
        ScmNodeStatus psRes = executor.getNodeStatus();
        return psRes.getStatusMap();
    }

    public ScmNodeInfo getNode(int port) throws ScmToolsException {
        scanConfDir();
        ScmNodeInfo node = node2Conf.get(port);
        if (node == null) {
            throw new ScmToolsException("Can't find conf path of " + port + " node",
                    ScmBaseExitCode.FILE_NOT_FIND);
        }
        return node;
    }

    public Map<Integer, ScmNodeInfo> getAllNode() {
        scanConfDir();
        return node2Conf;
    }

    public Map<Integer, ScmNodeInfo> getNodesByType(ScmNodeType type) {
        scanConfDir();
        return nodeType2Node.get(type);
    }

    private void scanConfDir() {
        if (node2Conf != null) {
            return;
        }
        node2Conf = new HashMap<>();
        nodeType2Node = new HashMap<ScmNodeType, Map<Integer, ScmNodeInfo>>();
        String confPath = ScmHelper.getPwd() + File.separator + ScmToolsDefine.FILE_NAME.CONF;

        for (ScmNodeType type : this.allNodeType) {
            scanNode(type, confPath + File.separator + type.getName());
        }
    }

    private void scanNode(ScmNodeType nodeType, String confPath) {
        File confFile = new File(confPath);
        if (!confFile.isDirectory()) {
            return;
        }
        File[] files = confFile.listFiles();
        Map<Integer, ScmNodeInfo> node2Conf = new HashMap<Integer, ScmNodeInfo>();
        for (File f : files) {
            if (f.isDirectory() && !f.getName().equals("samples")) {
                String applicationProp = confPath + File.separator + f.getName() + File.separator
                        + ScmToolsDefine.FILE_NAME.APP_PROPS;
                String confPort;
                try {
                    Properties prop = PropertiesUtil.loadProperties(applicationProp);
                    confPort = prop.getProperty(ScmToolsDefine.PROPERTIES.SERVER_PORT);
                }
                catch (ScmToolsException e1) {
                    logger.warn("scan conf dir have some incomplete node's conf file", e1);
                    continue;
                }
                if (confPort != null && !confPort.equals("")) {
                    try {
                        int port = Integer.valueOf(confPort);
                        node2Conf.put(port, new ScmNodeInfo(confPath + File.separator + f.getName(),
                                nodeType, port));
                        this.node2Conf.put(port, new ScmNodeInfo(
                                confPath + File.separator + f.getName(), nodeType, port));
                    }
                    catch (Exception e) {
                        logger.warn(
                                "scan conf dir have some incomplete node's conf file:failed to analyze server.port in conf file:"
                                        + applicationProp + ",server.port:" + confPort);
                    }
                }
                else {
                    logger.warn(
                            "scan conf dir have some incomplete node's conf file:Can't find server.port in conf file:"
                                    + applicationProp);
                }
            }
        }
        nodeType2Node.put(nodeType, node2Conf);
    }

    public void addMonitorNodeList(List<ScmNodeInfo> needAddList) {
        String daemonHomePath = null;
        try {
            // 如果找不到守护进程工具的路径，说明该节点目录与工具不在同一级目录下，且没有将该节点信息添加到监控表，那么就不需要监控
            daemonHomePath = findDaemonHomePath();
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
                        propPath);
                System.out.println("Failed to add node to monitor table,port:" + node.getPort()
                        + ",conf:" + propPath);
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
            if(e.getExitCode() == ScmBaseExitCode.FILE_NOT_FIND) {
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

    private String findDaemonHomePath() throws ScmToolsException {
        String daemonHomePath = ScmCommon.DAEMON_DIR_PATH;
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
