package com.sequoiacm.schedule.tools.command;

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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.sequoiacm.schedule.tools.SchCtl;
import com.sequoiacm.schedule.tools.common.ScmCommandUtil;
import com.sequoiacm.schedule.tools.common.ScmCommon;
import com.sequoiacm.schedule.tools.common.ScmHelpGenerator;
import com.sequoiacm.schedule.tools.common.ScmToolsDefine;
import com.sequoiacm.schedule.tools.element.ScmNodeInfo;
import com.sequoiacm.schedule.tools.element.ScmNodeType;
import com.sequoiacm.schedule.tools.exception.ScmExitCode;
import com.sequoiacm.schedule.tools.exception.ScmToolsException;
import com.sequoiacm.schedule.tools.exec.ScmExecutorWrapper;

public class ScmStartToolImpl implements ScmTool {
    private final int TIME_WAIT_PROCESS_RUNNING = 10000; // 10s
    private int waitProcessTimeout = 50000; // 50s

    private final String OPT_SHORT_PORT = "p";
    private final String OPT_LONG_PORT = "port";
    private final String OPT_SHORT_I = "I";
    private final String OPT_LONG_TIMEOUT = "timeout";
    // private final String OPT_LONG_OPTION = "option";
    private ScmExecutorWrapper executor;
    private int success = 0;
    private static Logger logger = LoggerFactory.getLogger(ScmStartToolImpl.class);
    private ScmHelpGenerator hp;
    private Options options;
    private RestTemplate restTemplate;

    public ScmStartToolImpl() throws ScmToolsException {
        options = new Options();
        hp = new ScmHelpGenerator();
        options.addOption(
                hp.createOpt(OPT_SHORT_PORT, OPT_LONG_PORT, "node port.", false, true, false));

        ScmCommandUtil.addTypeOption(options, hp, false, true);

        options.addOption(hp.createOpt(OPT_SHORT_I, null, "use current user.", false, false, true));
        options.addOption(hp.createOpt(null, OPT_LONG_TIMEOUT,
                "sets the starting timeout in seconds, default:50", false, true, false));

        executor = new ScmExecutorWrapper();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        restTemplate = new RestTemplate(factory);
    }

    @Override
    public void process(String[] args) throws ScmToolsException {
        SchCtl.checkHelpArgs(args);
        CommandLine commandLine = ScmCommandUtil.parseArgs(args, options);
        if (commandLine.hasOption(OPT_SHORT_PORT)
                && commandLine.hasOption(ScmCommandUtil.OPT_SHORT_NODE_TYPE)
                || !commandLine.hasOption(OPT_SHORT_PORT)
                        && !commandLine.hasOption(ScmCommandUtil.OPT_SHORT_NODE_TYPE)) {
            logger.error("Invalid arg:please set -" + ScmCommandUtil.OPT_SHORT_NODE_TYPE + " or -"
                    + OPT_SHORT_PORT);
            throw new ScmToolsException(
                    "please set -" + ScmCommandUtil.OPT_SHORT_NODE_TYPE + " or -" + OPT_SHORT_PORT,
                    ScmExitCode.INVALID_ARG);
        }

        if (commandLine.hasOption(OPT_LONG_TIMEOUT)) {
            waitProcessTimeout = ScmCommandUtil.getTimeout(commandLine, OPT_LONG_TIMEOUT);
        }

        Map<Integer, ScmNodeInfo> needStartMap = new HashMap<Integer, ScmNodeInfo>();
        // -p
        if (commandLine.hasOption(OPT_SHORT_PORT)) {
            String portString = commandLine.getOptionValue(OPT_SHORT_PORT);
            try {
                int port = ScmCommon.convertStrToInt(portString);
                ScmNodeInfo node = executor.getNode(port);
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
                needStartMap.putAll(executor.getAllNode());
                type = ScmToolsDefine.NODE_TYPE.ALL_STR;
            }
            else {
                ScmNodeType typeEnum = ScmNodeType.getNodeTypeByStr(type);
                Map<Integer, ScmNodeInfo> typeNodes = executor.getNodesByType(typeEnum);
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
        logger.info("Total:" + needStartMap.size() + ";Success:" + success + ";Failed:"
                + (needStartMap.size() - success));
        System.out.println("Total:" + needStartMap.size() + ";Success:" + success + ";Failed:"
                + (needStartMap.size() - success));
        if (!startRes || needStartMap.size() - success > 0) {
            throw new ScmToolsException(ScmExitCode.COMMON_UNKNOW_ERROR);
        }
    }

    private void startNodes(Map<Integer, ScmNodeInfo> needStartMap, List<ScmNodeInfo> checkList) {
        for (Integer key : needStartMap.keySet()) {
            try {
                int pid = executor.getNodePid(key);
                if (pid == -1) {
                    executor.startNode(needStartMap.get(key));
                    checkList.add(needStartMap.get(key));
                }
                else {
                    String status = getNodeRunningStatus(key, restTemplate);
                    if (status.equals("UP")) {
                        System.out.println("Suscess:" + needStartMap.get(key)
                                + " is already started (" + pid + ")");
                        logger.info("Suscess:" + needStartMap.get(key) + " is already started ("
                                + pid + ")");
                        success++;
                    }
                    else {
                        System.out
                                .println("Failed:" + needStartMap.get(key) + " is already started ("
                                        + pid + "),but node status is not normal");
                        logger.info("Failed:" + needStartMap.get(key) + " is already started ("
                                + pid + "),but node status is not normal:" + status);
                    }
                }
            }
            catch (ScmToolsException e) {
                logger.error(e.getMessage() + ",errorCode:" + e.getExitCode(), e);
                e.printErrorMsg();
            }
        }
    }

    private boolean isStartSuccess(List<ScmNodeInfo> checkList) throws ScmToolsException {
        long timeStamp = System.currentTimeMillis();
        boolean rc = true;
        Map<ScmNodeInfo, String> port2Status = new HashMap<>();

        while (true) {
            boolean isPidExist = false;
            Iterator<ScmNodeInfo> it = checkList.iterator();
            while (it.hasNext()) {
                ScmNodeInfo node = it.next();
                try {
                    int pid = executor.getNodePid(node.getPort());
                    if (pid != -1) {
                        isPidExist = true;
                        String runningStatus = getNodeRunningStatus(node.getPort(), restTemplate);
                        if (runningStatus.equals("UP")) {
                            System.out.println(
                                    "Suscess:" + node + " is successfully started (" + pid + ")");
                            logger.info(
                                    "Suscess:" + node + " is successfully started (" + pid + ")");
                            success++;
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

            ScmCommon.sleep(200);
        }

        for (Entry<ScmNodeInfo, String> entry : port2Status.entrySet()) {
            logger.error("failed to start node" + entry.getKey() + ",timeout,node status:"
                    + entry.getValue());
            System.out.println("Failed:" + entry.getKey() + " failed to start");
        }

        return false;
    }

    private String getNodeRunningStatus(int port, RestTemplate restTemplate) {
        // return "OK";
        try {
            Map<?, ?> resp = restTemplate.getForObject("http://localhost:" + port + "/health",
                    Map.class);
            return resp.get("status").toString().trim();
        }
        catch (Exception e) {
            return e.getMessage() + " " + Arrays.toString(e.getStackTrace());
        }
    }

    @Override
    public void printHelp(boolean isFullHelp) {
        hp.printHelp(isFullHelp);
    }
}
