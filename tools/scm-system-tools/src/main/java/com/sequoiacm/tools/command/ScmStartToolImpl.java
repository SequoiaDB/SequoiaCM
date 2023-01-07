package com.sequoiacm.tools.command;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmMonitorDaemonHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfoDetail;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeEnum;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import com.sequoiacm.infrastructure.tool.operator.ScmServiceNodeOperator;
import com.sequoiacm.tools.common.ScmSysToolUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScmStartToolImpl extends ScmTool {
    private static Logger logger = LoggerFactory.getLogger(ScmStartToolImpl.class);
    private final String OPT_LONG_TIMEOUT = "timeout";
    private int waitProcessTimeout = 200000; // 200s
    private final int SLEEP_TIME = 500;

    private ScmHelpGenerator hp;
    private Options options;
    private ScmNodeTypeList allNodeTypes ;
    private Map<ScmNodeType, ScmServiceNodeOperator> allOperators;
    private Map<ScmNodeType, Set<Integer>> needStartServiceNodes1st = new HashMap<>();
    private Map<ScmNodeType, Set<Integer>> needStartServiceNodes2st = new HashMap<>();
    private int totalNodeCount = 0;
    private int successNodeCount = 0;
    private long timeStamp;

    private ScmMonitorDaemonHelper executor = new ScmMonitorDaemonHelper();
    private String installPath;

    public ScmStartToolImpl() throws ScmToolsException{
        super("start");
        options = new Options();
        hp = new ScmHelpGenerator();

        allNodeTypes = ScmSysToolUtil.getAllNodeTypes();
        ScmCommandUtil.addTypeOptionForStartOrStopWithOutTypeNum(allNodeTypes, options, hp);

        options.addOption(hp.createOpt(null, OPT_LONG_TIMEOUT,
                "sets the starting timeout in seconds, default:200", false, true, false));

        installPath = Paths.get(ScmHelper.getPwd() + File.separator + "..").normalize().toString();
    }
    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.LOGBACK);
        allOperators = ScmSysToolUtil.initOperators(allNodeTypes, installPath);

        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);
        if ( !commandLine.hasOption(ScmCommandUtil.OPT_SHORT_NODE_TYPE) ) {
            logger.error("Invalid arg:please set -" + ScmCommandUtil.OPT_SHORT_NODE_TYPE);
            throw new ScmToolsException(
                    "please set -" + ScmCommandUtil.OPT_SHORT_NODE_TYPE ,
                    ScmBaseExitCode.INVALID_ARG);
        }

        if (commandLine.hasOption(OPT_LONG_TIMEOUT)) {
            waitProcessTimeout = ScmCommandUtil.getTimeout(commandLine, OPT_LONG_TIMEOUT);
        }

        List<ScmNodeType> startTypes = new ArrayList<>();
        String type = commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_NODE_TYPE).trim();
        if (type.equals(ScmToolsDefine.NODE_TYPE.ALL_STR)) {
            startTypes.addAll(allNodeTypes);
        }
        else {
            startTypes.add(allNodeTypes.getNodeTypeByStr(type));
        }

        Map<ScmNodeType, Set<Integer>> needStartServiceNodes = getAllServiceNodeStatus(
                startTypes);

        // 因为OM和fulltext依赖其他服务，需等待其他服务启动后才能启动，
        // 将启动节点分为两个批次，第一个批次包含除OM和fulltext的所有服务，第二个批次包含OM和fulltext
        // OM和fulltext的启动与其他服务解耦改进在 SEQUOIACM-1188 中跟踪，
        generateStartBatch(needStartServiceNodes);

        timeStamp = System.currentTimeMillis();
        startBatch(needStartServiceNodes1st);

        if (System.currentTimeMillis() - timeStamp < waitProcessTimeout) {
            startBatch(needStartServiceNodes2st);
        }

        // 输出未启动成功的节点状态
        outputUnStartedNodes();
        // 输出最终的统计结果
        int failedNodeCount = totalNodeCount - successNodeCount;
        logger.info("Total:" + totalNodeCount + ";Success:" + successNodeCount
                + ";Failed:" + failedNodeCount);
        System.out.println("Total:" + totalNodeCount + ";Success:" + successNodeCount
                + ";Failed:" + failedNodeCount);
        if (failedNodeCount > 0) {
            throw new ScmToolsException(ScmBaseExitCode.SYSTEM_ERROR);
        }
    }

    private Map<ScmNodeType, Set<Integer>> getAllServiceNodeStatus(List<ScmNodeType> needStartNodeTypes) throws ScmToolsException {
        Map<ScmNodeType, Set<Integer>> needCheckServiceNodes = new HashMap<>();
        List<ScmNodeInfo> startedNodeList = new ArrayList<>();

        for (ScmNodeType nodeType : needStartNodeTypes){
            ScmServiceNodeOperator operator = allOperators.get(nodeType);
            List<ScmNodeInfoDetail> nodeInfoDetailList = operator.getAllNodeInfoDetail();
            Set<Integer> startingPortSet = new HashSet<>();
            for (ScmNodeInfoDetail nodeInfoDetail : nodeInfoDetailList) {
                totalNodeCount++;
                if (nodeInfoDetail.getPid() != ScmNodeInfoDetail.NOT_RUNNING) {
                    String status = operator.getHealthDesc(nodeInfoDetail.getNodeInfo().getPort());
                    if (status.equals(ScmServiceNodeOperator.HEALTH_STATUS_UP)) {
                        successNodeCount++;
                        System.out.println("Success:" + nodeType.getName().toUpperCase() + "("
                                + nodeInfoDetail.getNodeInfo().getPort() + ")"
                                + " is already started (" + nodeInfoDetail.getPid()
                                + ")");
                        logger.info("Success:" + nodeType.getName().toUpperCase() + "("
                                + nodeInfoDetail.getNodeInfo().getPort() + ")"
                                + " is already started (" + nodeInfoDetail.getPid()
                                + ")");
                        startedNodeList.add(nodeInfoDetail.getNodeInfo());
                    }
                    else {
                        startingPortSet.add(nodeInfoDetail.getNodeInfo().getPort());
                    }
                }
                else {
                    startingPortSet.add(nodeInfoDetail.getNodeInfo().getPort());
                }
            }
            if (startingPortSet.size() > 0) {
                needCheckServiceNodes.put(nodeType, startingPortSet);
            }
        }

        if (startedNodeList.size() > 0) {
            executor.addMonitorNodeList(startedNodeList, installPath);
        }

        return needCheckServiceNodes;
    }

    private void generateStartBatch(Map<ScmNodeType, Set<Integer>> needCheckServiceNodes) {
        for (ScmNodeType nodeType : needCheckServiceNodes.keySet()) {
            if ( nodeType.getTypeEnum().equals(ScmNodeTypeEnum.OMSERVER)
                    || nodeType.getTypeEnum().equals(ScmNodeTypeEnum.FULLTEXTSERVER)) {
                needStartServiceNodes2st.put(nodeType, needCheckServiceNodes.get(nodeType));
            }
            else {
                needStartServiceNodes1st.put(nodeType, needCheckServiceNodes.get(nodeType));
            }
        }
    }

    private void startBatch(Map<ScmNodeType, Set<Integer>> needCheckServiceNodes)
            throws ScmToolsException {
        if (needCheckServiceNodes.size() <= 0) {
            return;
        }
        for (ScmNodeType nodeType : needCheckServiceNodes.keySet()) {
            allOperators.get(nodeType).startAll();
        }

        while (true) {
            checkStartingNodes(needCheckServiceNodes);
            if (needCheckServiceNodes.size() <= 0){
                break;
            }

            if (System.currentTimeMillis() - timeStamp > waitProcessTimeout) {
                break;
            }

            ScmCommon.sleep(SLEEP_TIME);
        }
    }

    private Map<ScmNodeInfoDetail, String> checkStartingNodes(Map<ScmNodeType, Set<Integer>> needCheckServiceNodes) throws ScmToolsException {
        Map<ScmNodeInfoDetail, String> startingNodes = new HashMap<>();
        List<ScmNodeInfo> startedNodeList = new ArrayList<>();

        Iterator<Map.Entry<ScmNodeType, Set<Integer>>> it = needCheckServiceNodes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ScmNodeType, Set<Integer>> service = it.next();
            ScmServiceNodeOperator operator = allOperators.get(service.getKey());
            Set<Integer> startingPortSet = service.getValue();
            Iterator<Integer> itPort = startingPortSet.iterator();
            while (itPort.hasNext()) {
                int port = itPort.next();
                ScmNodeInfoDetail nodeInfoDetail = operator.getNodeInfoDetail(port);
                if (nodeInfoDetail.getPid() != ScmNodeInfoDetail.NOT_RUNNING) {
                    String status = operator.getHealthDesc(nodeInfoDetail.getNodeInfo().getPort());
                    if (status.equals(ScmServiceNodeOperator.HEALTH_STATUS_UP)) {
                        successNodeCount++;
                        itPort.remove();
                        System.out.println("Success:" + nodeInfoDetail.getNodeInfo().getNodeType().getUpperName() + "("
                                + nodeInfoDetail.getNodeInfo().getPort() + ")" + " is successfully started (" + nodeInfoDetail.getPid()
                                + ")");
                        logger.info("Success:" + nodeInfoDetail.getNodeInfo().getNodeType().getUpperName() + "("
                                + nodeInfoDetail.getNodeInfo().getPort() + ")" + " is successfully started (" + nodeInfoDetail.getPid()
                                + ")");
                        startedNodeList.add(nodeInfoDetail.getNodeInfo());
                    }
                    else {
                        startingNodes.put(nodeInfoDetail, status);
                    }
                }
                else {
                    startingNodes.put(nodeInfoDetail, "can not find pid");
                }
            }

            if (startingPortSet.size() <= 0) {
                it.remove();
            }
        }

        if (startedNodeList.size() > 0) {
            executor.addMonitorNodeList(startedNodeList, installPath);
        }

        return startingNodes;
    }

    private void outputUnStartedNodes() throws ScmToolsException {
        if (needStartServiceNodes1st.size() > 0) {
            printUnStartedNodes(checkStartingNodes(needStartServiceNodes1st));
        }
        if (needStartServiceNodes2st.size() > 0) {
            printUnStartedNodes(checkStartingNodes(needStartServiceNodes2st));
        }
    }

    private void printUnStartedNodes(Map<ScmNodeInfoDetail, String> nodeInfoDetails) {
        for (Map.Entry<ScmNodeInfoDetail, String> entry : nodeInfoDetails.entrySet()) {
            logger.error("failed to start node"
                    + entry.getKey().getNodeInfo().getNodeType().getUpperName() + "("
                    + entry.getKey().getNodeInfo().getPort() + ")" + ",timeout,node status:"
                    + entry.getValue());
            System.out.println("Failed:" + entry.getKey().getNodeInfo().getNodeType().getUpperName()
                    + "(" + entry.getKey().getNodeInfo().getPort() + ")" + " failed to start");
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }
}
