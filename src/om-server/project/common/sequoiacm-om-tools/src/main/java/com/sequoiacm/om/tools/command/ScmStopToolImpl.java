package com.sequoiacm.om.tools.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.om.tools.OmCtl;
import com.sequoiacm.om.tools.common.ScmCommandUtil;
import com.sequoiacm.om.tools.common.ScmCommon;
import com.sequoiacm.om.tools.common.ScmHelpGenerator;
import com.sequoiacm.om.tools.common.ScmToolsDefine;
import com.sequoiacm.om.tools.element.ScmNodeInfo;
import com.sequoiacm.om.tools.element.ScmNodeType;
import com.sequoiacm.om.tools.exception.ScmExitCode;
import com.sequoiacm.om.tools.exception.ScmToolsException;
import com.sequoiacm.om.tools.exec.ScmExecutorWrapper;

public class ScmStopToolImpl implements ScmTool {
    private final String OPT_SHORT_PORT = "p";
    private final String OPT_LONG_PORT = "port";
    private final String OPT_LONG_FORCE = "force";
    private final String OPT_SHORT_FORCE = "f";

    private Options options;
    private ScmHelpGenerator hp;
    private ScmExecutorWrapper executor;
    private int success = 0;
    private static Logger logger = LoggerFactory.getLogger(ScmStopToolImpl.class);

    public ScmStopToolImpl() throws ScmToolsException {
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(hp.createOpt(OPT_SHORT_PORT, OPT_LONG_PORT, "node port.", false, true,
                false));

        ScmCommandUtil.addTypeOption(options, hp, false, true);

        options.addOption(hp.createOpt(OPT_SHORT_FORCE, OPT_LONG_FORCE, "force to stop node.",
                false, false, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        OmCtl.checkHelpArgs(args);
        executor = new ScmExecutorWrapper();

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
                logger.error("failed to stop node=" + commandLine.getOptionValue(OPT_SHORT_PORT), e);
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
                ScmNodeType typeEnum = ScmNodeType.getNodeTypeByStr(type);
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

        List<ScmNodeInfo> checkList = new ArrayList<>();
        stopNodes(needStopMap, checkList);

        // check stop resault and force option
        boolean stopRes = isStopSuccess(checkList, commandLine.hasOption(OPT_SHORT_FORCE));
        System.out.println("Total:" + needStopMap.size() + ";Success:" + success + ";Failed:"
                + (needStopMap.size() - success));
        logger.info("Total:" + needStopMap.size() + ";Success:" + success + ";Failed:"
                + (needStopMap.size() - success));
        if (!stopRes || needStopMap.size() != success) {
            throw new ScmToolsException(ScmExitCode.COMMON_UNKNOW_ERROR);
        }
    }

    private void stopNodes(Map<Integer, ScmNodeInfo> needStopMap, List<ScmNodeInfo> checkList) {
        for (Integer key : needStopMap.keySet()) {
            try {
                if (executor.getNodePid(key) != -1) {
                    executor.stopNode(key, false);
                    checkList.add(needStopMap.get(key));
                }
                else {
                    logger.info("Suscess:" + needStopMap.get(key) + " is already stopped");
                    System.out.println("Suscess:" + needStopMap.get(key) + " is already stopped");
                    success++;
                }
            }
            catch (ScmToolsException e) {
                logger.error("stop occur error", e);
                e.printErrorMsg();
            }
        }
    }

    private boolean isStopSuccess(List<ScmNodeInfo> checkList, boolean isForce)
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
                        logger.info("Suscess:" + node + " is successfully stopped");
                        System.out.println("Suscess:" + node + " is successfully stopped");
                        it.remove();
                        success++;
                    }
                }
                catch (ScmToolsException e) {
                    logger.error("check node status failed,node:" + node, e);
                    e.printErrorMsg();
                    rc = false;
                    it.remove();
                }
            }
            if (checkList.size() <= 0) {
                return rc;
            }
            if (System.currentTimeMillis() - timeStamp > 30000) {
                break;
            }
            ScmCommon.sleep(200);
        }

        if (isForce) {
            for (ScmNodeInfo node : checkList) {
                try {
                    logger.info("force stop node:" + node);
                    executor.stopNode(node.getPort(), true);
                }
                catch (ScmToolsException e) {
                    logger.error("force stop node occur exception,node:" + node, e);
                    System.out.println("force stop node occur exception,node:" + node + ",error:"
                            + e.getMessage());
                    rc = false;
                }
            }
            ScmCommon.sleep(200);
            Iterator<ScmNodeInfo> it = checkList.iterator();
            while (it.hasNext()) {
                ScmNodeInfo node = it.next();
                try {
                    int pid = executor.getNodePid(node.getPort());
                    if (pid == -1) {
                        logger.info("Suscess:" + node + " is successfully stopped");
                        System.out.println("Suscess:" + node + " is successfully stopped");
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
            logger.error("Failed:" + node + " failed to stop, timeout, node still running");
            System.out.println("Failed:" + node + " failed to stop");
        }
        return false;
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }

}
