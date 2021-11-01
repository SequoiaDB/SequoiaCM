package com.sequoiacm.tools.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmProcessInfo;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.infrastructure.tool.command.ScmTool;
import com.sequoiacm.infrastructure.tool.common.ScmHelpGenerator;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.common.ScmToolsDefine;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.common.ScmContentCommandUtil;
import com.sequoiacm.tools.common.ScmContentCommon;
import com.sequoiacm.tools.exception.ScmExitCode;
import com.sequoiacm.tools.exec.ScmExecutorWrapper;

public class ScmStartToolImpl extends ScmTool {
    public final int TIME_WAIT_PROCESS_RUNNING = 10000; // 10s

    public int TIME_WAIT_PROCESS_STATUS_NORMAL = 50000; // 50s
    private final String OPT_SHORT_ALL = "a";
    private final String OPT_LONG_ALL = "all";
    private final String OPT_SHORT_PORT = "p";
    private final String OPT_LONG_PORT = "port";
    private final String OPT_SHORT_I = "I";
    private final String OPT_SHORT_TIME_OUT = "t";
    private final String OPT_LONG_TIME_OUT = "timeout";
    // private final String OPT_LONG_OPTION = "option";
    private ScmExecutorWrapper executor;
    private List<Integer> startSuccessList = new ArrayList<>();
    private Map<Integer, String> needAddMap = new HashMap<>();
    private static Logger logger = LoggerFactory.getLogger(ScmStartToolImpl.class);
    private ScmHelpGenerator hp;
    private Options options;

    public ScmStartToolImpl() throws ScmToolsException {
        super("start");
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(
                hp.createOpt(OPT_SHORT_PORT, OPT_LONG_PORT, "node port.", false, true, false));

        options.addOption(
                hp.createOpt(OPT_SHORT_ALL, OPT_LONG_ALL, "start all node.", false, false, false));

        options.addOption(hp.createOpt(OPT_SHORT_TIME_OUT, OPT_LONG_TIME_OUT,
                "sets the starting timeout in seconds, default:50", false, true, false));

        // options.addOption(Option.builder().longOpt(OPT_LONG_OPTION).hasArgs().build());
        // hp.addOptHelp("--" + OPT_LONG_OPTION + " arg",
        // "start scm with extra options(--" + OPT_LONG_OPTION
        // + " 'extra option string'),\nexec 'java -h' for more options help");
        options.addOption(hp.createOpt(OPT_SHORT_I, null, "use current user.", false, false, true));

        executor = new ScmExecutorWrapper();
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        ScmHelper.configToolsLog(ScmToolsDefine.FILE_NAME.START_LOG_CONF);

        CommandLine commandLine = ScmContentCommandUtil.parseArgs(args, options);
        if (commandLine.hasOption(OPT_SHORT_PORT) && commandLine.hasOption(OPT_SHORT_ALL)
                || !commandLine.hasOption(OPT_SHORT_PORT)
                        && !commandLine.hasOption(OPT_SHORT_ALL)) {
            logger.error("Invalid arg:please set -" + OPT_SHORT_ALL + " or -" + OPT_SHORT_PORT);
            throw new ScmToolsException("please set -" + OPT_SHORT_ALL + " or -" + OPT_SHORT_PORT,
                    ScmExitCode.INVALID_ARG);
        }
        String contentServerJarFile = ScmContentCommon.getContentServerJarName();
        File f = new File(contentServerJarFile);
        if (!f.exists()) {
            logger.error("Can't find " + ScmContentCommon.getContenserverAbsolutePath()
                    + contentServerJarFile);
            throw new ScmToolsException("Can't find "
                    + ScmContentCommon.getContenserverAbsolutePath() + contentServerJarFile,
                    ScmExitCode.FILE_NOT_FIND);
        }

        if (commandLine.hasOption(OPT_SHORT_TIME_OUT)) {
            resetTimeout(commandLine);
        }

        Map<Integer, String> needStartMap = new HashMap<Integer, String>();
        // -p
        if (commandLine.hasOption(OPT_SHORT_PORT)) {
            String portString = commandLine.getOptionValue(OPT_SHORT_PORT);
            try {
                int port = ScmContentCommon.convertStrToInt(portString);
                String confPath = executor.getNodeConfPath(port);
                needStartMap.put(port, confPath);
            }
            catch (ScmToolsException e) {
                e.printErrorMsg();
                logger.error("Failed to start " + portString, e);
                System.out.println("Total:1;Success:0;Failed:1");
                throw new ScmToolsException(e.getExitCode());
            }
        }

        // -a
        else if (commandLine.hasOption(OPT_SHORT_ALL)) {
            needStartMap.putAll(executor.getAllNode());
            if (needStartMap.size() <= 0) {
                System.out.println("Can't find any node in conf path");
                System.out.println("Total:0;Success:0;Failed:0");
                logger.info("Can't find any node in conf path,start all node success");
                return;
            }
        }

        List<Integer> checkList = new ArrayList<>();
        startNodes(needStartMap, checkList);

        // check start res
        boolean startRes = isStartSuccess(checkList);
        for (Integer port : startSuccessList) {
            String confPath = needStartMap.get(port);
            needAddMap.put(port, confPath);
        }

        executor.addMonitorNodeList(needAddMap);

        logger.info("Total:" + needStartMap.size() + ";Success:" + startSuccessList.size()
                + ";Failed:" + (needStartMap.size() - startSuccessList.size()));
        System.out.println("Total:" + needStartMap.size() + ";Success:" + startSuccessList.size()
                + ";Failed:" + (needStartMap.size() - startSuccessList.size()));
        if (!startRes || needStartMap.size() - startSuccessList.size() > 0) {
            String startLogPath = new File(ScmContentCommon.START_LOG_PATH).getAbsolutePath();
            throw new ScmToolsException("please check log: " + startLogPath,
                    ScmExitCode.SYSTEM_ERROR);
        }
    }

    private void resetTimeout(CommandLine commandLine) throws ScmToolsException {
        int shortestTimeout = 5; // 5s
        String timeOut = commandLine.getOptionValue(OPT_SHORT_TIME_OUT);
        TIME_WAIT_PROCESS_STATUS_NORMAL = ScmContentCommon.convertStrToInt(timeOut);
        if (TIME_WAIT_PROCESS_STATUS_NORMAL < shortestTimeout) {
            logger.warn("rewrite timeout from {}s as {}s", TIME_WAIT_PROCESS_STATUS_NORMAL,
                    shortestTimeout);
            TIME_WAIT_PROCESS_STATUS_NORMAL = shortestTimeout;
        }

        TIME_WAIT_PROCESS_STATUS_NORMAL = TIME_WAIT_PROCESS_STATUS_NORMAL * 1000;
    }

    private void startNodes(Map<Integer, String> needStartMap, List<Integer> checkList) {
        for (Integer key : needStartMap.keySet()) {
            try {
                int pid = executor.getNodePid(key);
                if (pid == -1) {
                    executor.startNode(key);
                    checkList.add(key);
                }
                else {
                    String status = getNodeRunningStatus(key);
                    if (status.equals(CommonDefine.ScmProcessStatus.SCM_PROCESS_STATUS_RUNING)) {
                        System.out.println("Success:CONTENT-SERVER(" + key + ") is already started (" + pid + ")");
                        logger.info("Success:CONTENT-SERVER(" + key + ") is already started (" + pid + ")");
                        startSuccessList.add(key);
                    }
                    else {
                        System.out.println("Failed:CONTENT-SERVER(" + key + ") is already started ("
                                + pid + "),but node status is not normal");
                        logger.info("Failed:CONTENT-SERVER(" + key + ") is already started (" + pid
                                + "),but node status is not normal:" + status);
                    }
                }
            }
            catch (ScmToolsException e) {
                logger.error(e.getMessage() + ",errorCode:" + e.getExitCode(), e);
                e.printErrorMsg();
            }
        }
    }

    private boolean isStartSuccess(List<Integer> checkList) throws ScmToolsException {
        long timeStamp = System.currentTimeMillis();
        boolean rc = true;
        Map<Integer, String> port2Status = new HashMap<>();
        while (true) {
            boolean isPidExist = false;
            Iterator<Integer> it = checkList.iterator();
            while (it.hasNext()) {
                int p = it.next();
                try {
                    int pid = executor.getNodePid(p);
                    if (pid != -1) {
                        isPidExist = true;
                        String runningStatus = getNodeRunningStatus(p);
                        if (runningStatus
                                .equals(CommonDefine.ScmProcessStatus.SCM_PROCESS_STATUS_RUNING)) {
                            System.out.println("Success:CONTENT-SERVER(" + p + ") is successfully started (" + pid + ")");
                            logger.info("Success:CONTENT-SERVER(" + p + ") is successfully started (" + pid + ")");
                            startSuccessList.add(p);
                            it.remove();
                            port2Status.remove(p);
                        }
                        else {
                            port2Status.put(p, runningStatus);
                        }
                    }
                    else {
                        port2Status.put(p, "can not find pid");
                    }
                }
                catch (ScmToolsException e) {
                    e.printErrorMsg();
                    logger.error("check node status failed,port:" + p, e);
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

            if (isPidExist
                    && System.currentTimeMillis() - timeStamp > TIME_WAIT_PROCESS_STATUS_NORMAL) {
                break;
            }

            ScmContentCommon.sleep(200);
        }

        String logName = ScmContentCommon.SCM_LOG_DIR_NAME.replace("-", "") + ".log";

        for (Entry<Integer, String> entry : port2Status.entrySet()) {
            String logPath = ".." + File.separator + "log" + File.separator
                    + ScmContentCommon.SCM_LOG_DIR_NAME + File.separator + entry.getKey()
                    + File.separator + logName;
            String absLogPath = new File(logPath).getAbsolutePath();

            logger.error("failed to start node(" + entry.getKey() + "),timeout,node status:"
                    + entry.getValue() + ", please check log: {}", absLogPath);
            System.out.println("Failed:CONTENT-SERVER(" + entry.getKey() + ") failed to start");
        }

        return false;
    }

    private String getNodeRunningStatus(int port) {
        ScmSession ss = null;
        try {
            ss = ScmFactory.Session.createSession(SessionType.NOT_AUTH_SESSION,
                    new ScmConfigOption("localhost:" + port));
        }
        catch (Exception e) {
            return "failed to connect,error=" + e.toString() + ", stacktrace:"
                    + Arrays.toString(e.getStackTrace());
        }

        try {
            ScmProcessInfo processInfo = ScmSystem.ProcessInfo.getProcessInfo(ss);
            if (processInfo.getRunningStatus() == null) {
                return "runningStatus is null";
            }
            return processInfo.getRunningStatus();
        }
        catch (Exception e) {
            return "failed to get node status,error=" + e.toString() + ", stacktrace:"
                    + Arrays.toString(e.getStackTrace());
        }
        finally {
            ss.close();
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }
}
