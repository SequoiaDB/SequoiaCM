package com.sequoiacm.tools.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.common.ScmContentCommon;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exec.ScmExecutorWrapper;

public class ScmStopToolImpl extends ScmTool {
    private final String OPT_SHORT_PORT = "p";
    private final String OPT_LONG_PORT = "port";
    private final String OPT_SHORT_ALL = "a";
    private final String OPT_LONG_ALL = "all";
    private final String OPT_LONG_FORCE = "force";
    private final String OPT_SHORT_FORCE = "f";

    private Options options;
    private ScmHelpGenerator hp;
    private ScmExecutorWrapper executor;
    private int success = 0;
    private static Logger logger = LoggerFactory.getLogger(ScmStopToolImpl.class);

    public ScmStopToolImpl() throws ScmToolsException {
        super("stop");
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

        executor = new ScmExecutorWrapper();

        CommandLine commandLine = ScmContentCommandUtil.parseArgs(args, options);
        if (commandLine.hasOption(OPT_SHORT_PORT) && commandLine.hasOption(OPT_SHORT_ALL)
                || !commandLine.hasOption(OPT_SHORT_PORT)
                        && !commandLine.hasOption(OPT_SHORT_ALL)) {
            logger.error("Invalid arg:please set -a or -p");
            throw new ScmToolsException("please set -a or -p", ScmExitCode.INVALID_ARG);
        }

        Map<Integer, String> needStopMap = new HashMap<Integer, String>();
        // -p node
        if (commandLine.hasOption(OPT_SHORT_PORT)) {
            try {
                String portString = commandLine.getOptionValue(OPT_SHORT_PORT);
                int port = ScmContentCommon.convertStrToInt(portString);
                String nodeConf = executor.getNodeConfPath(port);
                needStopMap.put(port, nodeConf);
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
            needStopMap.putAll(executor.getAllNode());
            if (needStopMap.size() <= 0) {
                System.out.println("Can't find any node in conf path");
                logger.info("Can't find any node in conf path,stop success");
                System.out.println("Total:" + needStopMap.size() + ";Success:0;Failed:0");
                return;
            }
        }

        List<Integer> checkList = new ArrayList<>();
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

    private void stopNodes(Map<Integer, String> needStopMap, List<Integer> checkList) {
        for (Integer key : needStopMap.keySet()) {
            try {
                if (executor.getNodePid(key) != -1) {
                    executor.stopNode(key, false);
                    checkList.add(key);
                }
                else {
                    logger.info("Success:sequoiacm(" + key + ") is already stopped");
                    System.out.println("Success:sequoiacm(" + key + ") is already stopped");
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
                    int pid = executor.getNodePid(port);
                    if (pid == -1) {
                        logger.info("Success:sequoiacm(" + port + ") is successfully stopped");
                        System.out
                                .println("Success:sequoiacm(" + port + ") is successfully stopped");
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
                    executor.stopNode(port, true);
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
                    int pid = executor.getNodePid(port);
                    if (pid == -1) {
                        logger.info("Success:sequoiacm(" + port + ") is successfully stopped");
                        System.out
                                .println("Success:sequoiacm(" + port + ") is successfully stopped");
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
            logger.error(
                    "Failed:sequoiacm(" + port + ") failed to stop, timeout, node still running");
            System.out.println("Failed:sequoiacm(" + port + ") failed to stop");
        }
        return false;
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }

}
