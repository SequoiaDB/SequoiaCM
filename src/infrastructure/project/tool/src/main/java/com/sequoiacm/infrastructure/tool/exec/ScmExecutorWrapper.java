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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ScmExecutorWrapper {
    private String servicePath;
    private ScmExecutor executor;
    private Map<Integer, ScmNodeInfo> node2Conf;
    private static final Logger logger = LoggerFactory.getLogger(ScmExecutorWrapper.class);
    private ScmNodeType nodeType;

    public ScmExecutorWrapper(ScmNodeType nodeType, String installPath) throws ScmToolsException {
        this.nodeType = nodeType;
        this.servicePath = installPath + File.separator + nodeType.getServiceDirName();
        if (ScmCommon.isLinux()) {
            executor = new ScmLinuxExecutorImpl();
        }
        else {
            throw new ScmToolsException("Unsupport platform", ScmBaseExitCode.SYSTEM_ERROR);
        }
    }

    public void startNode(ScmNodeInfo node) throws ScmToolsException {
        String nodeConfPath = node.getConfPath();
        try {
            String springConfigLocation = nodeConfPath + File.separator
                    + ScmToolsDefine.FILE_NAME.APP_PROPS;
            String loggingConfig = nodeConfPath + File.separator + ScmToolsDefine.FILE_NAME.LOGBACK;

            String jarPath = ScmHelper.getJarPathByType(node.getNodeType(),
                    servicePath);
            String logPath = servicePath
                    + File.separator + ScmToolsDefine.FILE_NAME.LOG
                    + File.separator + node.getNodeType().getName() + File.separator
                    + node.getPort();
            String errorLogPath = logPath + File.separator + ScmToolsDefine.FILE_NAME.ERROR_OUT;
            ScmHelper.createDir(logPath);

            Properties sysPro = PropertiesUtil.loadProperties(springConfigLocation);
            String options = sysPro.getProperty(ScmToolsDefine.SCM_JVM_OPTIONS, "");

            executor.startNode(jarPath, springConfigLocation, loggingConfig, errorLogPath, options,
                    servicePath);
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
        String confOfPort = getNodeCheck(port).getConfPath();
        Map<String, ScmNodeProcessInfo> confNodeProcess;
        try {
            ScmNodeStatus psRes = executor.getNodeStatus(nodeType);
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
        ScmNodeStatus psRes = executor.getNodeStatus(nodeType);
        return psRes.getStatusMap();
    }

    public ScmNodeInfo getNodeCheck(int port) throws ScmToolsException {
        ScmNodeInfo node = getNode(port);
        if (node == null) {
            throw new ScmToolsException("Can't find conf path of " + port + " node",
                    ScmBaseExitCode.FILE_NOT_FIND);
        }
        return node;
    }

    public ScmNodeInfo getNode(int port) throws ScmToolsException {
        scanConfDir();
        ScmNodeInfo node = node2Conf.get(port);
        return node;
    }

    public Map<Integer, ScmNodeInfo> getAllNode() {
        scanConfDir();
        return node2Conf;
    }

    private void scanConfDir() {

        if (node2Conf != null) {
            return;
        }
        node2Conf = new HashMap<>();

        String confPath = servicePath + File.separator
                + ScmToolsDefine.FILE_NAME.CONF + File.separator + nodeType.getName();
        scanNode(confPath);

    }

    private void scanNode(String confPath) {
        File confFile = new File(confPath);
        if (!confFile.isDirectory()) {
            return;
        }

        File[] files = confFile.listFiles();
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
    }
}
