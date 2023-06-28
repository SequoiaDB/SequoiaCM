package com.sequoiacm.tools.command;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmMonitorDaemonHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.element.ScmNodeInfoDetail;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.common.ScmContentCommon;
import com.sequoiacm.tools.common.ScmContentServerNodeOperator;
import com.sequoiacm.tools.exception.ScmExitCode;

public class ScmStopToolImpl extends ScmTool {
    private final String OPT_SHORT_PORT = "p";
    private final String OPT_LONG_PORT = "port";
    private final String OPT_SHORT_ALL = "a";
    private final String OPT_LONG_ALL = "all";
    private final String OPT_LONG_FORCE = "force";
    private final String OPT_SHORT_FORCE = "f";
    private final String installPath;
    private final ScmContentServerNodeOperator operator;

    private Options options;
    private ScmHelpGenerator hp;
    private int success = 0;
    private static Logger logger = LoggerFactory.getLogger(ScmStopToolImpl.class);

    public ScmStopToolImpl() throws ScmToolsException {
        super("stop");
        this.installPath = ScmHelper.getAbsolutePathFromTool(
                ScmHelper.getPwd() + File.separator + ".." + File.separator + "..");
        operator = new ScmContentServerNodeOperator();
        operator.init(installPath);

        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(
                hp.createOpt(OPT_SHORT_PORT, OPT_LONG_PORT, "node port.", false, true, false));
        options.addOption(
                hp.createOpt(OPT_SHORT_ALL, OPT_LONG_ALL, "start all node.", false, false, false));
        options.addOption(hp.createOpt(OPT_SHORT_FORCE, OPT_LONG_FORCE, "force to stop node.",
                false, false, false));
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.STOP_LOG_CONF);

        CommandLine commandLine = ScmContentCommandUtil.parseArgs(args, options);
        if (commandLine.hasOption(OPT_SHORT_PORT) && commandLine.hasOption(OPT_SHORT_ALL)
                || !commandLine.hasOption(OPT_SHORT_PORT)
                        && !commandLine.hasOption(OPT_SHORT_ALL)) {
            logger.error("Invalid arg:please set -a or -p");
            throw new ScmToolsException("please set -a or -p", ScmExitCode.INVALID_ARG);
        }

        Map<Integer, ScmNodeInfo> needStopNode = new HashMap<>();
        // -p node
        if (commandLine.hasOption(OPT_SHORT_PORT)) {
            try {
                String portString = commandLine.getOptionValue(OPT_SHORT_PORT);
                int port = ScmContentCommon.convertStrToInt(portString);
                ScmNodeInfo node = operator.getNodeInfo(port);
                if (node == null) {
                    throw new ScmToolsException("Can't find conf path of " + port + " node",
                            ScmExitCode.FILE_NOT_FIND);
                }
                needStopNode.put(node.getPort(), node);
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
        else if (commandLine.hasOption(OPT_SHORT_ALL)) {
            List<ScmNodeInfo> nodes = operator.getAllNodeInfo();
            for (ScmNodeInfo nodeInfo : nodes) {
                needStopNode.put(nodeInfo.getPort(), nodeInfo);
            }
            if (needStopNode.size() <= 0) {
                System.out.println("Can't find any node in conf path");
                logger.info("Can't find any node in conf path,stop success");
                System.out.println("Total:" + needStopNode.size() + ";Success:0;Failed:0");
                return;
            }
        }

        ScmMonitorDaemonHelper.changeMonitorStatus(needStopNode.keySet(), "off", installPath);

        List<Integer> checkList = new ArrayList<>();
        stopNodes(needStopNode, checkList);

        // check stop resault and force option
        boolean stopRes = isStopSuccess(checkList, commandLine.hasOption(OPT_SHORT_FORCE));
        System.out.println("Total:" + needStopNode.size() + ";Success:" + success + ";Failed:"
                + (needStopNode.size() - success));
        logger.info("Total:" + needStopNode.size() + ";Success:" + success + ";Failed:"
                + (needStopNode.size() - success));
        if (!stopRes || needStopNode.size() != success) {
            throw new ScmToolsException("please check log: " + ScmCommon.getStopLogPath(),
                    ScmExitCode.SYSTEM_ERROR);
        }
    }

    private void stopNodes(Map<Integer, ScmNodeInfo> needStopMap, List<Integer> checkList) {
        for (Integer key : needStopMap.keySet()) {
            try {
                if (operator.getNodeInfoDetail(key).getPid() != ScmNodeInfoDetail.NOT_RUNNING) {
                    operator.stop(key, false);
                    checkList.add(key);
                }
                else {
                    logger.info("Success:CONTENT-SERVER(" + key + ") is already stopped");
                    System.out.println("Success:CONTENT-SERVER(" + key + ") is already stopped");
                    success++;
                }
            }
            catch (ScmToolsException e) {
                logger.error("stop occur error", e);
                e.printErrorMsg();
            }
        }
    }

    private boolean isStopSuccess(List<Integer> checkList, boolean isForce)
            throws ScmToolsException {
        boolean rc = true;
        long timeStamp = System.currentTimeMillis();
        while (true) {
            Iterator<Integer> it = checkList.iterator();
            while (it.hasNext()) {
                int port = it.next();
                try {
                    int pid = operator.getNodeInfoDetail(port).getPid();
                    if (pid == ScmNodeInfoDetail.NOT_RUNNING) {
                        logger.info("Success:CONTENT-SERVER(" + port + ") is successfully stopped");
                        System.out.println(
                                "Success:CONTENT-SERVER(" + port + ") is successfully stopped");
                        it.remove();
                        success++;
                    }
                }
                catch (ScmToolsException e) {
                    logger.error("check node status failed,port:" + port, e);
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
            ScmContentCommon.sleep(200);
        }

        if (isForce) {
            for (int port : checkList) {
                try {
                    logger.info("force stop node:" + port);
                    operator.stop(port, true);
                }
                catch (ScmToolsException e) {
                    logger.error("force stop node occur exception,port:" + port, e);
                    System.out.println("force stop node occur exception,port:" + port + ",error:"
                            + e.getMessage());
                    rc = false;
                }
            }
            ScmContentCommon.sleep(200);
            Iterator<Integer> it = checkList.iterator();
            while (it.hasNext()) {
                Integer port = it.next();
                try {
                    int pid = operator.getNodeInfoDetail(port).getPid();
                    if (pid == ScmNodeInfoDetail.NOT_RUNNING) {
                        logger.info("Success:CONTENT-SERVER(" + port + ") is successfully stopped");
                        System.out.println(
                                "Success:CONTENT-SERVER(" + port + ") is successfully stopped");
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

        for (int port : checkList) {
            String nodeLog = ScmCommon.getServiceInstallPath() + File.separator + "log"
                    + File.separator + "content-server" + File.separator + port + File.separator
                    + "contentserver.log";
            logger.error("Failed:CONTENT-SERVER(" + port
                    + ") failed to stop, timeout, node still running, check log for detail: "
                    + nodeLog);
            System.out.println("Failed:CONTENT-SERVER(" + port + ") failed to stop");
        }
        return false;
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }

}
