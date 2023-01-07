package com.sequoiacm.infrastructure.tool.command;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmMonitorDaemonHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfoDetail;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;
import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperatorGroup;

public class ScmStartToolImpl extends ScmTool {
    private final int TIME_WAIT_PROCESS_RUNNING = 10000; // 10s
    private final ScmServiceNodeOperatorGroup nodeOperators;
    private final String installPath;
    protected int waitProcessTimeout = 50000; // 50s
    protected int SLEEP_TIME = 200;

    private final String OPT_SHORT_I = "I";
    private final String OPT_LONG_TIMEOUT = "timeout";
    // private final String OPT_LONG_OPTION = "option";
    private List<ScmNodeInfo> startSuccessList = new ArrayList<>();
    private static Logger logger = LoggerFactory.getLogger(ScmStartToolImpl.class);
    private ScmHelpGenerator hp;
    private Options options;

    public ScmStartToolImpl(List<ScmServiceNodeOperator> nodeOperatorList)
            throws ScmToolsException {
        super("start");
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

        options.addOption(hp.createOpt(OPT_SHORT_I, null, "use current user.", false, false, true));
        options.addOption(hp.createOpt(null, OPT_LONG_TIMEOUT,
                "sets the starting timeout in seconds, default:50", false, true, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        // 日志
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.START_LOG_CONF);

        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);
        if (commandLine.hasOption(ScmCommandUtil.OPT_SHORT_PORT)
                && commandLine.hasOption(ScmCommandUtil.OPT_SHORT_NODE_TYPE)
                || !commandLine.hasOption(ScmCommandUtil.OPT_SHORT_PORT)
                        && !commandLine.hasOption(ScmCommandUtil.OPT_SHORT_NODE_TYPE)) {
            logger.error("Invalid arg:please set -" + ScmCommandUtil.OPT_SHORT_NODE_TYPE + " or -"
                    + ScmCommandUtil.OPT_SHORT_PORT);
            throw new ScmToolsException(
                    "please set -" + ScmCommandUtil.OPT_SHORT_NODE_TYPE + " or -"
                            + ScmCommandUtil.OPT_SHORT_PORT,
                    ScmBaseExitCode.INVALID_ARG);
        }

        if (commandLine.hasOption(OPT_LONG_TIMEOUT)) {
            waitProcessTimeout = ScmCommandUtil.getTimeout(commandLine, OPT_LONG_TIMEOUT);
        }


        Map<Integer, ScmNodeInfo> needStartMap = new HashMap<Integer, ScmNodeInfo>();
        // -p
        if (commandLine.hasOption(ScmCommandUtil.OPT_SHORT_PORT)) {
            String portString = commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_PORT);
            try {
                int port = ScmCommon.convertStrToInt(portString);
                ScmNodeInfo node = nodeOperators.getNodeInfo(port);
                needStartMap.put(port, node);
            }
            catch (ScmToolsException e) {
                e.printErrorMsg();
                logger.error("Failed to start " + portString, e);
                System.out.println("Total:1;Success:0;Failed:1");
                throw new ScmToolsException(e.getExitCode());
            }
        }

        // -t
        else if (commandLine.hasOption(ScmCommandUtil.OPT_SHORT_NODE_TYPE)) {
            String type = commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_NODE_TYPE).trim();
            if (type.equals(ScmToolsDefine.NODE_TYPE.ALL_NUM)
                    || type.equals(ScmToolsDefine.NODE_TYPE.ALL_STR)) {
                needStartMap.putAll(nodeOperators.getAllNode());
                type = ScmToolsDefine.NODE_TYPE.ALL_STR;
            }
            else {
                ScmNodeType typeEnum = this.nodeOperators.getSupportTypes().getNodeTypeByStr(type);
                Map<Integer, ScmNodeInfo> typeNodes = nodeOperators.getNodesByType(typeEnum);
                if (typeNodes != null) {
                    needStartMap.putAll(typeNodes);
                }
                type = typeEnum.toString();
            }

            if (needStartMap.size() <= 0) {
                System.out.println("Can't find any node in conf path,type:" + type);
                System.out.println("Total:0;Success:0;Failed:0");
                logger.info("Can't find any node in conf path,start all node success,type=" + type);
                return;
            }
        }

        List<ScmNodeInfo> checkList = new ArrayList<>();
        startNodes(needStartMap, checkList);

        // check start res
        boolean startRes = isStartSuccess(checkList);
        String isIgnoreDaemon = System.getenv(ScmCommon.IGNORE_DAEMON_ENV);
        if (isIgnoreDaemon == null) {
            ScmMonitorDaemonHelper.addMonitorNodeList(startSuccessList, installPath);
        }

        logger.info("Total:" + needStartMap.size() + ";Success:" + startSuccessList.size()
                + ";Failed:" + (needStartMap.size() - startSuccessList.size()));
        System.out.println("Total:" + needStartMap.size() + ";Success:" + startSuccessList.size()
                + ";Failed:" + (needStartMap.size() - startSuccessList.size()));
        if (!startRes || needStartMap.size() - startSuccessList.size() > 0) {
            throw new ScmToolsException(ScmBaseExitCode.SYSTEM_ERROR);
        }
    }

    protected void startNodes(Map<Integer, ScmNodeInfo> needStartMap, List<ScmNodeInfo> checkList) {
        for (Integer key : needStartMap.keySet()) {
            try {
                int pid = nodeOperators.getNodePid(key);
                if (pid == ScmNodeInfoDetail.NOT_RUNNING) {
                    nodeOperators.startNode(needStartMap.get(key).getPort());
                    checkList.add(needStartMap.get(key));
                }
                else {
                    String status = nodeOperators.getHealthDesc(key);
                    if (status.equals(ScmServiceNodeOperator.HEALTH_STATUS_UP)) {
                        System.out.println(
                                "Success:" + needStartMap.get(key).getNodeType().getUpperName()
                                        + "(" + needStartMap.get(key).getPort() + ")"
                                        + " is already started (" + pid + ")");
                        logger.info("Success:" + needStartMap.get(key).getNodeType().getUpperName()
                                + "(" + needStartMap.get(key).getPort() + ")"
                                + " is already started (" + pid + ")");
                        startSuccessList.add(needStartMap.get(key));
                    }
                    else {
                        System.out.println("Failed:"
                                + needStartMap.get(key).getNodeType().getUpperName() + "("
                                + needStartMap.get(key).getPort() + ")" + " is already started ("
                                + pid + "),but node status is not normal");
                        logger.info("Failed:" + needStartMap.get(key).getNodeType().getUpperName()
                                + "(" + needStartMap.get(key).getPort() + ")"
                                + " is already started (" + pid + "),but node status is not normal:"
                                + status);
                    }
                }
            }
            catch (ScmToolsException e) {
                logger.error(e.getMessage() + ",errorCode:" + e.getExitCode(), e);
                e.printErrorMsg();
            }
        }
    }

    protected boolean isStartSuccess(List<ScmNodeInfo> checkList) throws ScmToolsException {
        long timeStamp = System.currentTimeMillis();
        boolean rc = true;
        Map<ScmNodeInfo, String> port2Status = new HashMap<>();

        while (true) {
            boolean isPidExist = false;
            Iterator<ScmNodeInfo> it = checkList.iterator();
            while (it.hasNext()) {
                ScmNodeInfo node = it.next();
                try {
                    int pid = nodeOperators.getNodePid(node.getPort());
                    if (pid != ScmNodeInfoDetail.NOT_RUNNING) {
                        isPidExist = true;
                        String runningStatus = nodeOperators.getHealthDesc(node.getPort());
                        if (runningStatus.equals(ScmServiceNodeOperator.HEALTH_STATUS_UP)) {
                            System.out.println("Success:" + node.getNodeType().getUpperName() + "("
                                    + node.getPort() + ")" + " is successfully started (" + pid
                                    + ")");
                            logger.info("Success:" + node.getNodeType().getUpperName() + "("
                                    + node.getPort() + ")" + " is successfully started (" + pid
                                    + ")");
                            startSuccessList.add(node);
                            it.remove();
                            port2Status.remove(node);
                        }
                        else {
                            port2Status.put(node, runningStatus);
                        }
                    }
                    else {
                        port2Status.put(node, "can not find pid");
                    }
                }
                catch (ScmToolsException e) {
                    e.printErrorMsg();
                    logger.error("check node status failed,port:" + node.getPort(), e);
                    it.remove();
                    rc = false;
                }
            }
            if (checkList.size() <= 0) {
                return rc;
            }

            if (!isPidExist && System.currentTimeMillis() - timeStamp > TIME_WAIT_PROCESS_RUNNING) {
                break;
            }

            if (isPidExist && System.currentTimeMillis() - timeStamp > waitProcessTimeout) {
                break;
            }

            ScmCommon.sleep(SLEEP_TIME);
        }

        for (Entry<ScmNodeInfo, String> entry : port2Status.entrySet()) {
            logger.error("failed to start node" + entry.getKey().getNodeType().getUpperName() + "("
                    + entry.getKey().getPort() + ")" + ",timeout,node status:" + entry.getValue());
            System.out.println("Failed:" + entry.getKey().getNodeType().getUpperName() + "("
                    + entry.getKey().getPort() + ")" + " failed to start");
        }

        return false;
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }

}
