package com.sequoiacm.infrastructure.tool.command;

import com.sequoiacm.infrastructure.tool.common.*;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeList;
import com.sequoiacm.infrastructure.tool.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.infrastructure.tool.exec.ScmExecutorWrapper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ScmStopToolImpl extends ScmTool {
    protected final String OPT_SHORT_PORT = "p";
    protected final String OPT_LONG_PORT = "port";
    protected final String OPT_LONG_FORCE = "force";
    protected final String OPT_SHORT_FORCE = "f";

    protected int SLEEP_TIME = 200;
    protected int STOP_TIMEOUT = 30 * 1000;

    protected Options options;
    protected ScmHelpGenerator hp;
    protected ScmExecutorWrapper executor;
    protected ScmNodeTypeList nodeTypes;
    protected int success = 0;
    protected static Logger logger = LoggerFactory.getLogger(ScmStopToolImpl.class);

    public ScmStopToolImpl(ScmNodeTypeList nodeTypes) throws ScmToolsException {
        super("stop");
        this.nodeTypes = nodeTypes;
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(
                hp.createOpt(OPT_SHORT_PORT, OPT_LONG_PORT, "node port.", false, true, false));

        ScmCommandUtil.addTypeOptionForStartOrStop(nodeTypes, options, hp, false, true);

        options.addOption(hp.createOpt(OPT_SHORT_FORCE, OPT_LONG_FORCE, "force to stop node.",
                false, false, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        // 日志
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.STOP_LOG_CONF);

        executor = new ScmExecutorWrapper(this.nodeTypes);
        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);
        if (commandLine.hasOption(OPT_SHORT_PORT)
                && commandLine.hasOption(ScmCommandUtil.OPT_SHORT_NODE_TYPE)
                || !commandLine.hasOption(OPT_SHORT_PORT)
                        && !commandLine.hasOption(ScmCommandUtil.OPT_SHORT_NODE_TYPE)) {
            logger.error("Invalid arg:please set -t or -p");
            throw new ScmToolsException("please set -t or -p", ScmExitCode.INVALID_ARG);
        }

        Map<Integer, ScmNodeInfo> needStopMap = new HashMap<Integer, ScmNodeInfo>();
        // -p node
        if (commandLine.hasOption(OPT_SHORT_PORT)) {
            try {
                String portString = commandLine.getOptionValue(OPT_SHORT_PORT);
                int port = ScmCommon.convertStrToInt(portString);
                ScmNodeInfo nodeInfo = executor.getNode(port);
                needStopMap.put(port, nodeInfo);
            }
            catch (ScmToolsException e) {
                e.printErrorMsg();
                logger.error("failed to stop node=" + commandLine.getOptionValue(OPT_SHORT_PORT),
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
                needStopMap.putAll(executor.getAllNode());
                type = "all";
            }
            else {
                ScmNodeType typeEnum = this.nodeTypes.getNodeTypeByStr(type);
                Map<Integer, ScmNodeInfo> typeNodes = executor.getNodesByType(typeEnum);
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

        executor.changeMonitorStatus(needStopMap.keySet(), "off");

        List<ScmNodeInfo> checkList = new ArrayList<>();
        stopNodes(needStopMap, checkList);

        // check stop resault and force option
        boolean stopRes = isStopSuccess(checkList, commandLine.hasOption(OPT_SHORT_FORCE));
        System.out.println("Total:" + needStopMap.size() + ";Success:" + success + ";Failed:"
                + (needStopMap.size() - success));
        logger.info("Total:" + needStopMap.size() + ";Success:" + success + ";Failed:"
                + (needStopMap.size() - success));
        if (!stopRes || needStopMap.size() != success) {
            throw new ScmToolsException(ScmExitCode.COMMON_UNKNOWN_ERROR);
        }
    }

    protected void stopNodes(Map<Integer, ScmNodeInfo> needStopMap, List<ScmNodeInfo> checkList) {
        for (Integer key : needStopMap.keySet()) {
            try {
                if (executor.getNodePid(key) != -1) {
                    executor.stopNode(key, false);
                    checkList.add(needStopMap.get(key));
                }
                else {
                    logger.info("Success:" + needStopMap.get(key).getNodeType().getUpperName() + "("
                            + needStopMap.get(key).getPort() + ")" + " is already stopped");
                    System.out.println(
                            "Success:" + needStopMap.get(key).getNodeType().getUpperName() + "("
                                    + needStopMap.get(key).getPort() + ")" + " is already stopped");
                    success++;
                }
            }
            catch (ScmToolsException e) {
                logger.error("stop occur error", e);
                e.printErrorMsg();
            }
        }
    }

    protected boolean isStopSuccess(List<ScmNodeInfo> checkList, boolean isForce)
            throws ScmToolsException {
        boolean rc = true;
        long timeStamp = System.currentTimeMillis();
        while (true) {
            Iterator<ScmNodeInfo> it = checkList.iterator();
            while (it.hasNext()) {
                ScmNodeInfo node = it.next();
                try {
                    int pid = executor.getNodePid(node.getPort());
                    if (pid == -1) {
                        logger.info("Success:" + node.getNodeType().getUpperName() + "("
                                + node.getPort() + ")" + " is successfully stopped");
                        System.out.println("Success:" + node.getNodeType().getUpperName() + "("
                                + node.getPort() + ")" + " is successfully stopped");
                        it.remove();
                        success++;
                    }
                }
                catch (ScmToolsException e) {
                    logger.error("check node status failed,node:"
                            + node.getNodeType().getUpperName() + "(" + node.getPort() + ")", e);
                    e.printErrorMsg();
                    rc = false;
                    it.remove();
                }
            }
            if (checkList.size() <= 0) {
                return rc;
            }
            if (System.currentTimeMillis() - timeStamp > STOP_TIMEOUT) {
                break;
            }
            ScmCommon.sleep(SLEEP_TIME);
        }

        if (isForce) {
            for (ScmNodeInfo node : checkList) {
                try {
                    logger.info("force stop node:" + node.getNodeType().getUpperName() + "("
                            + node.getPort() + ")");
                    executor.stopNode(node.getPort(), true);
                }
                catch (ScmToolsException e) {
                    logger.error("force stop node occur exception,node:"
                            + node.getNodeType().getUpperName() + "(" + node.getPort() + ")", e);
                    System.out.println("force stop node occur exception,node:"
                            + node.getNodeType().getUpperName() + "(" + node.getPort() + ")"
                            + ",error:" + e.getMessage());
                    rc = false;
                }
            }
            ScmCommon.sleep(SLEEP_TIME);
            Iterator<ScmNodeInfo> it = checkList.iterator();
            while (it.hasNext()) {
                ScmNodeInfo node = it.next();
                try {
                    int pid = executor.getNodePid(node.getPort());
                    if (pid == -1) {
                        logger.info("Success:" + node.getNodeType().getUpperName() + "("
                                + node.getPort() + ")" + " is successfully stopped");
                        System.out.println("Success:" + node.getNodeType().getUpperName() + "("
                                + node.getPort() + ")" + " is successfully stopped");
                        it.remove();
                        success++;
                    }
                }
                catch (ScmToolsException e) {
                    logger.error("stop node occur exception", e);
                    e.printErrorMsg();
                    it.remove();
                    rc = false;
                }
            }
        }
        if (checkList.size() == 0) {
            return rc;
        }

        for (ScmNodeInfo node : checkList) {
            logger.error("Failed:" + node.getNodeType().getUpperName() + "(" + node.getPort() + ")"
                    + " failed to stop, timeout, node still running");
            System.out.println("Failed:" + node.getNodeType().getUpperName() + "(" + node.getPort()
                    + ")" + " failed to stop");
        }
        return false;
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }
}
