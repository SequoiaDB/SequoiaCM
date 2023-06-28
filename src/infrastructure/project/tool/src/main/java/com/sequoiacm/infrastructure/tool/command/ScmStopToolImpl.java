package com.sequoiacm.infrastructure.tool.command;

import com.sequoiacm.infrastructure.tool.common.*;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfoDetail;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperatorGroup;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class ScmStopToolImpl extends ScmTool {
    protected final String OPT_LONG_FORCE = "force";
    protected final String OPT_SHORT_FORCE = "f";
    private final ScmServiceNodeOperatorGroup nodeOperators;
    private final String installPath;

    protected int SLEEP_TIME = 200;
    protected int STOP_TIMEOUT = 30 * 1000;
    protected int STOP_INTERVAL_TIME = 100;

    protected Options options;
    protected ScmHelpGenerator hp;
    protected int success = 0;
    protected static Logger logger = LoggerFactory.getLogger(ScmStopToolImpl.class);

    public ScmStopToolImpl(List<ScmServiceNodeOperator> nodeOperatorList) throws ScmToolsException {
        super("stop");
        this.nodeOperators = new ScmServiceNodeOperatorGroup(nodeOperatorList);
        this.installPath = ScmHelper
                .getAbsolutePathFromTool(ScmHelper.getPwd() + File.separator + "..");
        nodeOperators.init(installPath);

        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(
                hp.createOpt(ScmCommandUtil.OPT_SHORT_PORT, ScmCommandUtil.OPT_LONG_PORT,
                        "node port.", false, true, false));

        ScmCommandUtil.addTypeOptionForStartOrStop(nodeOperators.getSupportTypes(), options, hp,
                false, true);

        options.addOption(hp.createOpt(OPT_SHORT_FORCE, OPT_LONG_FORCE, "force to stop node.",
                false, false, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        // 日志
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.STOP_LOG_CONF);
        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);
        if (commandLine.hasOption(ScmCommandUtil.OPT_SHORT_PORT)
                && commandLine.hasOption(ScmCommandUtil.OPT_SHORT_NODE_TYPE)
                || !commandLine.hasOption(ScmCommandUtil.OPT_SHORT_PORT)
                        && !commandLine.hasOption(ScmCommandUtil.OPT_SHORT_NODE_TYPE)) {
            logger.error("Invalid arg:please set -t or -p");
            throw new ScmToolsException("please set -t or -p", ScmBaseExitCode.INVALID_ARG);
        }

        Map<Integer, ScmNodeInfo> needStopMap = new HashMap<Integer, ScmNodeInfo>();
        // -p node
        if (commandLine.hasOption(ScmCommandUtil.OPT_SHORT_PORT)) {
            try {
                String portString = commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_PORT);
                int port = ScmCommon.convertStrToInt(portString);
                ScmNodeInfo nodeInfo = nodeOperators.getNodeInfo(port);
                needStopMap.put(port, nodeInfo);
            }
            catch (ScmToolsException e) {
                e.printErrorMsg();
                logger.error("failed to stop node="
                        + commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_PORT),
                        e);
                System.out.println("Total:1;Success:0;Failed:1");
                logger.info("Total:1;Success:0;Failed:1");
                throw new ScmToolsException(e.getExitCode());
            }
        }

        // --all
        else if (commandLine.hasOption(ScmCommandUtil.OPT_SHORT_NODE_TYPE)) {
            String type = commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_NODE_TYPE).trim();
            if (type.equals(ScmToolsDefine.NODE_TYPE.ALL_NUM)
                    || type.equals(ScmToolsDefine.NODE_TYPE.ALL_STR)) {
                needStopMap.putAll(nodeOperators.getAllNode());
                type = "all";
            }
            else {
                ScmNodeType typeEnum = nodeOperators.getSupportTypes().getNodeTypeByStr(type);
                Map<Integer, ScmNodeInfo> typeNodes = nodeOperators.getNodesByType(typeEnum);
                if (typeNodes != null) {
                    needStopMap.putAll(typeNodes);
                }
                type = typeEnum.toString();
            }
            if (needStopMap.size() <= 0) {
                System.out.println("Can't find any node in conf path,type:" + type);
                logger.info("Can't find any node in conf path,stop success,type:" + type);
                System.out.println("Total:" + needStopMap.size() + ";Success:0;Failed:0");
                return;
            }
        }

        ScmMonitorDaemonHelper.changeMonitorStatus(needStopMap.keySet(), "off", installPath);

        stopNodes(needStopMap, commandLine.hasOption(OPT_SHORT_FORCE));
    }

    protected void stopNodes(Map<Integer, ScmNodeInfo> needStopMap, boolean isForce)
            throws ScmToolsException {
        List<ScmNodeInfo> nodeInfoList = getSortedStopList(needStopMap);
        List<ScmNodeInfo> timeOutList = new ArrayList<>();
        List<ScmNodeInfo> checkStatusList = new ArrayList<>();
        for (ScmNodeInfo node : nodeInfoList) {
            try {
                if (nodeOperators.getNodePid(node.getPort()) != ScmNodeInfoDetail.NOT_RUNNING) {
                    // not force: kill -15 pid
                    nodeOperators.stopNode(node.getPort(), false);
                    checkStatusList.add(node);
                    // 睡眠 100 ms，尽量避免后执行stop的节点比先执行stop的节点先停止，导致出错
                    ScmCommon.sleep(STOP_INTERVAL_TIME);
                }
                else {
                    logger.info("Success:" + node.getNodeType().getUpperName() + "("
                            + node.getPort() + ")" + " is already stopped");
                    System.out.println("Success:" + node.getNodeType().getUpperName() + "("
                            + node.getPort() + ")" + " is already stopped");
                    success++;
                }
            }
            catch (ScmToolsException e) {
                logger.error("Failed:" + node.getNodeType().getUpperName() + "(" + node.getPort()
                        + ")" + " failed to stop, stop occur error", e);
                System.out.println("Failed:" + node.getNodeType().getUpperName() + "("
                        + node.getPort() + ")" + " failed to stop");
            }
        }

        for (ScmNodeInfo node : checkStatusList) {
            // check node status
            NodeStopStatus checkResult = checkNodeStoppingStatus(node, STOP_TIMEOUT);
            if (isForce && checkResult == NodeStopStatus.TIME_OUT) {
                // force: kill -15 pid still running then kill -9 pid
                stopForce(node);
                ScmCommon.sleep(SLEEP_TIME);
                checkResult = checkNodeStoppingStatus(node, 0);
            }

            if (checkResult == NodeStopStatus.NOT_RUNNING) {
                logger.info("Success:" + node.getNodeType().getUpperName() + "(" + node.getPort()
                        + ")" + " is successfully stopped");
                System.out.println("Success:" + node.getNodeType().getUpperName() + "("
                        + node.getPort() + ")" + " is successfully stopped");
                success++;
            }
            else if (checkResult == NodeStopStatus.FAILED) {
                logger.error("Failed:" + node.getNodeType().getUpperName() + "(" + node.getPort()
                        + ")" + " failed to stop, check node status failed, unknown node status");
                System.out.println(
                        "Failed:" + node.getNodeType().getUpperName() + "(" + node.getPort() + ")"
                                + " failed to stop, check node status failed, unknown node status");
            }
            else {
                timeOutList.add(node);
            }
        }

        // time out some node still running
        for (ScmNodeInfo node : timeOutList) {
            String nodeLog = ScmCommon.getServiceInstallPath() + File.separator + "log"
                    + File.separator + node.getNodeType().getName() + File.separator
                    + node.getPort() + File.separator
                    + (node.getNodeType().getName().replace("-", "")) + ".log";
            logger.error("Failed:" + node.getNodeType().getUpperName() + "(" + node.getPort() + ")"
                    + " failed to stop, timeout, node still running, check log for detail: "
                    + nodeLog);
            System.out.println("Failed:" + node.getNodeType().getUpperName() + "(" + node.getPort()
                    + ")" + " failed to stop");
        }

        System.out.println("Total:" + nodeInfoList.size() + ";Success:" + success + ";Failed:"
                + (nodeInfoList.size() - success));
        logger.info("Total:" + nodeInfoList.size() + ";Success:" + success + ";Failed:"
                + (nodeInfoList.size() - success));

        if (nodeInfoList.size() != success) {
            throw new ScmToolsException("please check log: " + ScmCommon.getStopLogPath(),
                    ScmBaseExitCode.SYSTEM_ERROR);
        }
    }

    private NodeStopStatus checkNodeStoppingStatus(ScmNodeInfo node, int stopTimeOut) {
        long timeStamp = System.currentTimeMillis();
        while (true) {
            NodeStopStatus status = getNodeStopStatus(node);
            if (status == NodeStopStatus.NOT_RUNNING || status == NodeStopStatus.FAILED) {
                return status;
            }
            if (System.currentTimeMillis() - timeStamp > stopTimeOut) {
                break;
            }
            ScmCommon.sleep(SLEEP_TIME);
        }
        return NodeStopStatus.TIME_OUT;
    }

    private void stopForce(ScmNodeInfo node) throws ScmToolsException {
        // force stop node
        try {
            logger.info("force stop node:" + node.getNodeType().getUpperName() + "("
                    + node.getPort() + ")");
            nodeOperators.stopNode(node.getPort(), true);
        }
        catch (ScmToolsException e) {
            logger.error("force stop node occur exception,node:" + node.getNodeType().getUpperName()
                    + "(" + node.getPort() + ")", e);
            System.out.println(
                    "force stop node occur exception,node:" + node.getNodeType().getUpperName()
                            + "(" + node.getPort() + ")" + ",error:" + e.getMessage());
            throw e;
        }
    }

    private NodeStopStatus getNodeStopStatus(ScmNodeInfo node) {
        try {
            int pid = nodeOperators.getNodePid(node.getPort());
            if (pid == ScmNodeInfoDetail.NOT_RUNNING) {
                return NodeStopStatus.NOT_RUNNING;
            }
            return NodeStopStatus.RUNNING;
        }
        catch (ScmToolsException e) {
            logger.error("check node status failed,node:" + node.getNodeType().getUpperName() + "("
                    + node.getPort() + ")", e);
            return NodeStopStatus.FAILED;
        }
    }

    protected List<ScmNodeInfo> getSortedStopList(Map<Integer, ScmNodeInfo> needStopMap) {
        Collection<ScmNodeInfo> values = needStopMap.values();
        List<ScmNodeInfo> nodeInfoList = new ArrayList<>(values);
        // 优先级越小，优先部署，逆序排序，先部署的后停止
        Collections.sort(nodeInfoList, new Comparator<ScmNodeInfo>() {
            @Override
            public int compare(ScmNodeInfo n1, ScmNodeInfo n2) {
                return n2.getNodeType().getTypeEnum().getDeployPriority()
                        - n1.getNodeType().getTypeEnum().getDeployPriority();
            }
        });
        return nodeInfoList;
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }
}
