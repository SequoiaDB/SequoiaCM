package com.sequoiacm.tools.command;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmMonitorDaemonHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfoDetail;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
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

public class ScmStopToolImpl extends ScmTool {
    private static Logger logger = LoggerFactory.getLogger(ScmStopToolImpl.class);
    private final String OPT_LONG_TIMEOUT = "timeout";
    private int waitProcessTimeout = 30000; // 30s
    private final int SLEEP_TIME = 200; // ms
    protected final String OPT_LONG_FORCE = "force";
    protected final String OPT_SHORT_FORCE = "f";

    private ScmHelpGenerator hp;
    private Options options;
    private ScmNodeTypeList allNodeTypes;
    private Map<ScmNodeType, ScmServiceNodeOperator> allOperators = new HashMap<>();
    private Map<ScmNodeType, Set<Integer>> needStopServiceNodes = new HashMap<>();

    private int totalNodeCount = 0;
    private int successNodeCount = 0;
    private long timeStamp = 0;
    private ScmMonitorDaemonHelper executor = new ScmMonitorDaemonHelper();
    private String installPath;

    public ScmStopToolImpl() throws ScmToolsException {
        super("stop");

        options = new Options();
        hp = new ScmHelpGenerator();

        allNodeTypes = ScmSysToolUtil.getAllNodeTypes();
        ScmCommandUtil.addTypeOptionForStartOrStopWithOutTypeNum(allNodeTypes, options, hp);

        options.addOption(hp.createOpt(null, OPT_LONG_TIMEOUT,
                "sets the starting timeout in seconds, default: " + waitProcessTimeout / 1000,
                false, true, false));
        options.addOption(hp.createOpt(OPT_SHORT_FORCE, OPT_LONG_FORCE, "force to stop node.",
                false, false, false));

        installPath = Paths.get(ScmHelper.getPwd() + File.separator + "..").normalize().toString();
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.LOGBACK);
        allOperators = ScmSysToolUtil.initOperators(allNodeTypes, installPath);

        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);
        if (!commandLine.hasOption(ScmCommandUtil.OPT_SHORT_NODE_TYPE)) {
            logger.error("Invalid arg:please set -" + ScmCommandUtil.OPT_SHORT_NODE_TYPE);
            throw new ScmToolsException("please set -" + ScmCommandUtil.OPT_SHORT_NODE_TYPE,
                    ScmBaseExitCode.INVALID_ARG);
        }
        boolean forceStop = commandLine.hasOption(OPT_SHORT_FORCE);
        if (commandLine.hasOption(OPT_LONG_TIMEOUT)) {
            waitProcessTimeout = ScmCommandUtil.getTimeout(commandLine, OPT_LONG_TIMEOUT);
        }

        List<ScmServiceNodeOperator> stopOperators = new ArrayList();
        String type = commandLine.getOptionValue(ScmCommandUtil.OPT_SHORT_NODE_TYPE).trim();
        if (type.equals(ScmToolsDefine.NODE_TYPE.ALL_STR)) {
            stopOperators.addAll(allOperators.values());
        }
        else {
            ScmNodeType nodeType = allNodeTypes.getNodeTypeByStr(type);
            stopOperators.add(allOperators.get(nodeType));
        }

        needStopServiceNodes = getAllNeedStopServiceNode(stopOperators);
        timeStamp = System.currentTimeMillis();
        stopService();

        if (needStopServiceNodes.size() > 0 && forceStop) {
            forceStopService();
        }

        outputUnStoppedNodes();

        // 输出全部结果
        int failedNodeCount = totalNodeCount - successNodeCount;
        logger.info("Total:" + totalNodeCount + ";Success:" + successNodeCount + ";Failed:"
                + failedNodeCount);
        System.out.println("Total:" + totalNodeCount + ";Success:" + successNodeCount + ";Failed:"
                + failedNodeCount);
        if (failedNodeCount > 0) {
            throw new ScmToolsException(ScmBaseExitCode.SYSTEM_ERROR);
        }
    }

    private Map<ScmNodeType, Set<Integer>> getAllNeedStopServiceNode(
            List<ScmServiceNodeOperator> stopOperators) throws ScmToolsException {
        Map<ScmNodeType, Set<Integer>> needCheckServiceNodes = new HashMap<>();

        for (ScmServiceNodeOperator operator : stopOperators) {
            List<ScmNodeInfoDetail> nodeInfoDetailList = operator.getAllNodeInfoDetail();
            Set<Integer> needStopPortSet = new HashSet<>();
            for (ScmNodeInfoDetail nodeInfoDetail : nodeInfoDetailList) {
                totalNodeCount++;
                if (nodeInfoDetail.getPid() == ScmNodeInfoDetail.NOT_RUNNING) {
                    successNodeCount++;
                    logger.info("Success:" + operator.getNodeType().getUpperName() + "("
                            + nodeInfoDetail.getNodeInfo().getPort() + ")" + " is already stopped");
                    System.out.println("Success:" + operator.getNodeType().getUpperName() + "("
                            + nodeInfoDetail.getNodeInfo().getPort() + ")" + " is already stopped");
                }
                else {
                    needStopPortSet.add(nodeInfoDetail.getNodeInfo().getPort());
                }
            }
            if (needStopPortSet.size() > 0) {
                needCheckServiceNodes.put(operator.getNodeType(), needStopPortSet);
                executor.changeMonitorStatus(needStopPortSet, "off", installPath);
            }
        }

        return needCheckServiceNodes;
    }

    private void stopService() throws ScmToolsException {
        for (ScmNodeType nodeType : needStopServiceNodes.keySet()) {
            ScmServiceNodeOperator operator = allOperators.get(nodeType);
            Set<Integer> needStopPorts = needStopServiceNodes.get(nodeType);
            Iterator<Integer> itPort = needStopPorts.iterator();
            while (itPort.hasNext()) {
                int port = itPort.next();
                try {
                    operator.stop(port, false);
                }
                catch (ScmToolsException e) {
                    logger.error("stop occur error", e);
                    e.printErrorMsg();
                    itPort.remove();
                }
            }
        }

        while (true) {
            checkStoppingNodes();
            if (needStopServiceNodes.size() <= 0) {
                break;
            }

            if (System.currentTimeMillis() - timeStamp > waitProcessTimeout) {
                break;
            }

            ScmCommon.sleep(SLEEP_TIME);
        }
    }

    private void forceStopService() throws ScmToolsException {
        Iterator<Map.Entry<ScmNodeType, Set<Integer>>> it = needStopServiceNodes.entrySet()
                .iterator();
        while (it.hasNext()) {
            Map.Entry<ScmNodeType, Set<Integer>> needCheckService = it.next();
            allOperators.get(needCheckService.getKey()).stopAll(true);
        }
        ScmCommon.sleep(SLEEP_TIME);

        while (true) {
            checkStoppingNodes();
            if (needStopServiceNodes.size() <= 0) {
                break;
            }

            ScmCommon.sleep(SLEEP_TIME);
        }
    }

    private void checkStoppingNodes() throws ScmToolsException {
        Iterator<Map.Entry<ScmNodeType, Set<Integer>>> it = needStopServiceNodes.entrySet()
                .iterator();
        while (it.hasNext()) {
            Map.Entry<ScmNodeType, Set<Integer>> service = it.next();
            Set<Integer> stoppingPortSet = service.getValue();
            ScmServiceNodeOperator operator = allOperators.get(service.getKey());
            Iterator<Integer> itPort = stoppingPortSet.iterator();
            while (itPort.hasNext()) {
                int port = itPort.next();
                ScmNodeInfoDetail nodeInfoDetail = operator.getNodeInfoDetail(port);
                if (nodeInfoDetail.getPid() == ScmNodeInfoDetail.NOT_RUNNING) {
                    successNodeCount++;
                    itPort.remove();
                    logger.info(
                            "Success:" + nodeInfoDetail.getNodeInfo().getNodeType().getUpperName()
                                    + "(" + nodeInfoDetail.getNodeInfo().getPort() + ")"
                                    + " is successfully stopped");
                    System.out.println(
                            "Success:" + nodeInfoDetail.getNodeInfo().getNodeType().getUpperName()
                                    + "(" + nodeInfoDetail.getNodeInfo().getPort() + ")"
                                    + " is successfully stopped");
                }
            }

            if (stoppingPortSet.size() <= 0) {
                it.remove();
            }
        }

        return;
    }

    private void outputUnStoppedNodes() throws ScmToolsException {
        if (needStopServiceNodes.size() > 0) {
            Iterator<Map.Entry<ScmNodeType, Set<Integer>>> it = needStopServiceNodes.entrySet()
                    .iterator();
            while (it.hasNext()) {
                Map.Entry<ScmNodeType, Set<Integer>> service = it.next();
                Set<Integer> stoppingPortSet = service.getValue();
                for (Integer port : stoppingPortSet) {
                    logger.error("Failed:" + service.getKey().getUpperName() + "(" + port + ")"
                            + " failed to stop, timeout, node still running");
                    System.out.println("Failed:" + service.getKey().getUpperName() + "(" + port
                            + ")" + " failed to stop");
                }
            }
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }
}
