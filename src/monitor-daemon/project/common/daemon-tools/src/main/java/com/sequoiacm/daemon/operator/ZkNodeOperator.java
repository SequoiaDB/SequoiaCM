package com.sequoiacm.daemon.operator;

import com.sequoiacm.daemon.common.CommonUtils;
import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.element.ScmNodeInfo;
import com.sequoiacm.daemon.exec.ScmExecutor;
import com.sequoiacm.infrastructure.tool.common.PropertiesUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

public class ZkNodeOperator implements NodeOperator {
    private static final Logger logger = LoggerFactory.getLogger(ZkNodeOperator.class);
    private ScmExecutor executor;

    public ZkNodeOperator(ScmExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean isNodeRunning(ScmNodeInfo node) throws ScmToolsException {
        String confPath = node.getConfPath();
        Properties prop = PropertiesUtil.loadProperties(confPath);
        String dataDir = prop.getProperty(DaemonDefine.ZK_DATA_DIR);
        if (dataDir == null) {
            throw new ScmToolsException(DaemonDefine.ZK_DATA_DIR + " in zookeeper cfg is null",
                    ScmBaseExitCode.INVALID_ARG);
        }
        String pidFilePath = dataDir + File.separator + DaemonDefine.ZOO_PID_FILE;
        File pidFile = new File(pidFilePath);
        if (!pidFile.exists()) {
            return false;
        }
        int pid = getPid(pidFile);
        return executor.getPid(pid + "") != -1;
    }

    private int getPid(File pidFile) throws ScmToolsException {
        int pid = -1;
        BufferedReader bfr = null;
        FileReader fr = null;
        try {
            fr = new FileReader(pidFile);
            bfr = new BufferedReader(fr);
            while (true) {
                String line = bfr.readLine();
                if (line == null) {
                    break;
                }
                String pidStr = line.trim();
                pid = ScmCommon.convertStrToInt(pidStr);
            }
            return pid;
        }
        catch (FileNotFoundException e) {
            return -1;
        }
        catch (IOException e) {
            throw new ScmToolsException("Failed to get zookeeper pid", ScmBaseExitCode.SYSTEM_ERROR, e);
        }
        finally {
            CommonUtils.closeResource(bfr);
            CommonUtils.closeResource(fr);
        }
    }

    @Override
    public void startNode(ScmNodeInfo node) throws ScmToolsException {
        String confPath = node.getConfPath();
        String servicePath = confPath.substring(0, confPath.indexOf("/" + DaemonDefine.CONF));
        String shellName = node.getServerType().getShellName();
        String cmd = servicePath + File.separator + "bin" + File.separator + shellName + " start "
                + confPath;
        executor.execCmd(cmd);
    }

    @Override
    public List<ScmNodeInfo> getNodeInfos(File serviceDir) throws ScmToolsException {
        List<ScmNodeInfo> nodeList = new ArrayList<>();
        String confPath = serviceDir.getAbsolutePath() + File.separator + DaemonDefine.CONF;
        File confDir = new File(confPath);
        for (File file : Objects.requireNonNull(confDir.listFiles())) {
            if (file.isDirectory()) {
                continue;
            }

            String pattern = DaemonDefine.ZOO_PATTERN;
            if (Pattern.matches(pattern, file.getName())) {
                ScmNodeInfo scmNodeInfo = zkCfgToNodeInfo(file);
                nodeList.add(scmNodeInfo);
            }
        }
        return nodeList;
    }

    private ScmNodeInfo zkCfgToNodeInfo(File file) throws ScmToolsException {
        Properties prop = PropertiesUtil.loadProperties(file);
        String clientPortStr = prop.getProperty(DaemonDefine.CLIENT_PORT);
        int clientPort = ScmCommon.convertStrToInt(clientPortStr);

        ScmNodeInfo nodeInfo = new ScmNodeInfo();
        nodeInfo.setServerType(ScmServerScriptEnum.ZOOKEEPER);
        nodeInfo.setPort(clientPort);
        nodeInfo.setConfPath(file.getAbsolutePath());

        String status = isNodeRunning(nodeInfo) ? DaemonDefine.NODE_STATUS_ON
                : DaemonDefine.NODE_STATUS_OFF;
        nodeInfo.setStatus(status);

        return nodeInfo;
    }

    @Override
    public int getNodePort(ScmNodeInfo node) throws ScmToolsException {
        String confPath = node.getConfPath();
        Properties prop = PropertiesUtil.loadProperties(confPath);
        String clientPortStr = prop.getProperty(DaemonDefine.CLIENT_PORT);
        return ScmCommon.convertStrToInt(clientPortStr);
    }
}
