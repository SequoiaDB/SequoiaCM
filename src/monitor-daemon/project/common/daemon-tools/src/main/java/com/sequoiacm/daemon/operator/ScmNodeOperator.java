package com.sequoiacm.daemon.operator;

import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.element.ScmCmdResult;
import com.sequoiacm.daemon.element.ScmNodeInfo;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.daemon.exec.ScmExecutor;
import com.sequoiacm.infrastructure.tool.common.PropertiesUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.element.ScmServerScriptEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ScmNodeOperator implements NodeOperator {
    private ScmExecutor executor;

    public ScmNodeOperator(ScmExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean isNodeRunning(ScmNodeInfo node) throws ScmToolsException {
        String confPath = node.getConfPath();
        String dirName = node.getServerType().getDirName();
        String servicePath = confPath.substring(0, confPath.indexOf(dirName) + dirName.length());
        int port = node.getPort();
        String shellName = node.getServerType().getShellName();

        String cmd = servicePath + File.separator + "bin" + File.separator + shellName
                + " list -m local -p " + port;
        ScmCmdResult result = executor.execCmd(cmd);
        return getNodePid(result.getStdIn()) != -1;
    }

    private int getNodePid(List<String> stdInList) throws ScmToolsException {
        // CONFIG-SERVER(8190) (8866)
        String nodeLine = null;
        int count = 0;
        for (String line : stdInList) {
            if (line.startsWith("Total")) {
                continue;
            }
            count++;
            nodeLine = line;
        }

        if (count != 1) {
            throw new ScmToolsException("Get one node pid, but return multiple pid",
                    ScmExitCode.INVALID_ARG);
        }

        try {
            String pidStr = nodeLine.substring(nodeLine.lastIndexOf("(") + 1,
                    nodeLine.lastIndexOf(")"));
            if (pidStr.equals("-")) {
                return -1;
            }
            return ScmCommon.convertStrToInt(pidStr);
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to get node pid", ScmExitCode.INVALID_ARG, e);
        }
    }

    @Override
    public void startNode(ScmNodeInfo node) throws ScmToolsException {
        int port = node.getPort();
        String shellName = node.getServerType().getShellName();
        String dirName = node.getServerType().getDirName();
        String confPath = node.getConfPath();
        String servicePath = confPath.substring(0, confPath.indexOf(dirName) + dirName.length());
        String cmd = DaemonDefine.EXPORT_IGNORE_DAEMON_ENV + servicePath + File.separator + "bin" + File.separator + shellName
                + " start -p " + port;
        executor.execCmd(cmd);
    }

    @Override
    public List<ScmNodeInfo> getNodeInfos(File serviceDir) throws ScmToolsException {
        String servicePath = serviceDir.getAbsolutePath();
        String dirName = serviceDir.getName();
        String shellName = ScmServerScriptEnum.getShellNameByDirName(dirName);
        // 如果该目录不属于scm的服务目录，那么就跳过
        if (shellName == null) {
            return null;
        }

        String cmd = servicePath + File.separator + "bin" + File.separator + shellName
                + " list -m local -l";
        ScmCmdResult result = executor.execCmd(cmd);
        return stdInToNodeList(result.getStdIn());
    }

    private List<ScmNodeInfo> stdInToNodeList(List<String> stdInputList) throws ScmToolsException {
        if (stdInputList == null || stdInputList.size() == 0) {
            return null;
        }
        List<ScmNodeInfo> nodeList = new ArrayList<>();
        for (String line : stdInputList) {
            if (line.startsWith("Total")) {
                continue;
            }
            ScmNodeInfo scmNodeInfo = parseNodeLine(line);
            nodeList.add(scmNodeInfo);
        }
        return nodeList;
    }

    private ScmNodeInfo parseNodeLine(String line) throws ScmToolsException {
        // e.g : GATEWAY(8080) (98437)
        // /sequoiacm/sequoiacm-cloud/conf/gateway/8080/application.properties
        ScmNodeInfo scmNodeInfo = new ScmNodeInfo();
        try {
            String[] sub = line.split(" ");
            String[] sub0 = sub[0].split("\\(");
            scmNodeInfo.setServerType(ScmServerScriptEnum.getEnumByType(sub0[0]));
            String portStr = sub0[1].substring(0, sub0[1].length() - 1);
            int port = ScmCommon.convertStrToInt(portStr);
            scmNodeInfo.setPort(port);
            scmNodeInfo.setConfPath(sub[2]);
            String pid = sub[1].substring(0, sub[1].length() - 1);
            if (pid.equals("-")) {
                scmNodeInfo.setStatus(DaemonDefine.NODE_STATUS_OFF);
            }
            else {
                scmNodeInfo.setStatus(DaemonDefine.NODE_STATUS_ON);
            }
        }
        catch (Exception e) {
            throw new ScmToolsException("Failed to parse nodeLine to nodeInfo,line:{" + line + "}",
                    ScmExitCode.INVALID_ARG, e);
        }
        return scmNodeInfo;
    }

    @Override
    public int getNodePort(ScmNodeInfo node) throws ScmToolsException {
        String confPath = node.getConfPath();
        Properties prop = PropertiesUtil.loadProperties(confPath);
        String serverPortStr = prop.getProperty(DaemonDefine.SERVER_PORT);
        return ScmCommon.convertStrToInt(serverPortStr);
    }
}
