package com.sequoiacm.config.tools.exec;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.config.tools.common.PropertiesUtil;
import com.sequoiacm.config.tools.common.ScmCommon;
import com.sequoiacm.config.tools.common.ScmHelper;
import com.sequoiacm.config.tools.common.ScmToolsDefine;
import com.sequoiacm.config.tools.element.ScmNodeInfo;
import com.sequoiacm.config.tools.element.ScmNodeProcessInfo;
import com.sequoiacm.config.tools.element.ScmNodeStatus;
import com.sequoiacm.config.tools.element.ScmNodeType;
import com.sequoiacm.config.tools.exception.ScmExitCode;
import com.sequoiacm.config.tools.exception.ScmToolsException;

public class ScmExecutorWrapper {
    private ScmExecutor executor;
    private Map<Integer, ScmNodeInfo> node2Conf;
    private Map<ScmNodeType, Map<Integer, ScmNodeInfo>> nodeType2Node;
    private static final Logger logger = LoggerFactory.getLogger(ScmExecutorWrapper.class);

    public ScmExecutorWrapper() throws ScmToolsException {
        if (ScmCommon.isLinux()) {
            executor = new ScmLinuxExecutorImpl();
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
            throw new ScmToolsException("Failed:sequoiacm(" + node.getPort() + ") failed to start:"
                    + e.getMessage(), e.getExitCode());
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
            throw new ScmToolsException("Failed to check " + port + " node status:"
                    + e.getMessage(), e.getExitCode());
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
                    ScmExitCode.FILE_NOT_FIND);
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

        for (ScmNodeType type : ScmNodeType.values()) {
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
                        node2Conf.put(port, new ScmNodeInfo(
                                confPath + File.separator + f.getName(), nodeType, port));
                        this.node2Conf.put(port,
                                new ScmNodeInfo(confPath + File.separator + f.getName(), nodeType,
                                        port));
                    }
                    catch (Exception e) {
                        logger.warn("scan conf dir have some incomplete node's conf file:failed to analyze server.port in conf file:"
                                + applicationProp + ",server.port:" + confPort);
                    }
                }
                else {
                    logger.warn("scan conf dir have some incomplete node's conf file:Can't find server.port in conf file:"
                            + applicationProp);
                }
            }
        }
        nodeType2Node.put(nodeType, node2Conf);
    }
}
