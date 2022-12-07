package com.sequoiacm.diagnose.command;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.element.ScmCheckConnTarget;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmCheckConnResult;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.diagnose.execption.LogCollectException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.common.ScmHelper;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Command
public class ScmConnCheck extends SubCommand {

    public static final String NAME = "conn-check";

    public static final String GATEWAY = "gateway";

    public static ScmSession session = null;

    private static final String LOGBACK_PATH = "logback-path";

    public static Map<String, List<ScmServiceInstance>> ipCheckMap = new HashMap();

    private static final Logger logger = LoggerFactory.getLogger(ScmConnCheck.class);

    @Override
    public void run(String[] args) throws Exception {
        Options ops = addParam();
        CommandLine commandLine = new DefaultParser().parse(ops, args, false);

        if (commandLine.hasOption("help")) {
            printHelp();
            System.exit(0);
        }

        String logbackPath = commandLine.getOptionValue(LOGBACK_PATH);
        changeLogbackFile(logbackPath);
        String gatewayAddress = commandLine.getOptionValue(GATEWAY);
        try {
            createSession(gatewayAddress);
            List<ScmServiceInstance> serviceInstanceList = getServerInstance();

            printClusterInfoAndNetworkCheck(serviceInstanceList);
            System.out.println("Begin to checking the network segment of the nodes...");
            if (ipCheckMap.size() == 1) {
                System.out.println("\nThe nodes network segment is "
                        + ipCheckMap.keySet().iterator().next() + ".*.*");
                doCheck(serviceInstanceList);
            }
            else {
                printMultipleNetwork();
                for (String key : ipCheckMap.keySet()) {
                    System.out.println("\nThe nodes network segment is " + key + ".*.*");
                    List<ScmServiceInstance> scmServiceInstanceList = ipCheckMap.get(key);
                    doCheck(scmServiceInstanceList);
                }
            }
        }
        finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public static void createSession(String gatewayPath) throws ScmException {
        ScmConfigOption tmpScmConfigOption = new ScmConfigOption(gatewayPath);
        session = ScmFactory.Session.createSession(ScmType.SessionType.NOT_AUTH_SESSION,
                tmpScmConfigOption);
    }

    private static List<ScmCheckConnResult> getFailedResult(List<ScmCheckConnResult> allResult) {
        ArrayList<ScmCheckConnResult> errorResult = new ArrayList<>();
        if (allResult == null || allResult.size() < 1) {
            return errorResult;
        }
        for (ScmCheckConnResult connResult : allResult) {
            if (!connResult.isConnected()) {
                errorResult.add(connResult);
            }
        }
        return errorResult;
    }

    private List<ScmServiceInstance> getServerInstance() throws ScmException {
        List<ScmServiceInstance> serviceList = ScmSystem.ServiceCenter
                .getServiceInstanceList(session, null);
        Collections.sort(serviceList, new Comparator<ScmServiceInstance>() {
            @Override
            public int compare(ScmServiceInstance o1, ScmServiceInstance o2) {
                if (o2.getIp().equals(o1.getIp())) {
                    if (o2.getServiceName().equals(o1.getServiceName())) {
                        return o1.getPort() - o2.getPort();
                    }
                    return o1.getServiceName().compareTo(o2.getServiceName());
                }
                return o1.getIp().compareTo(o2.getIp());
            }
        });
        return serviceList;
    }

    private void doCheck(List<ScmServiceInstance> serviceInstanceList) throws ScmException {
        int errorCount = 0;
        int totalCount = 0;
        String[] nodes = instanceListToNodeList(serviceInstanceList);
        ScmCheckConnTarget target = ScmCheckConnTarget.builder().instance(nodes).build();
        System.out.println("Begin to checking the connectivity of the nodes... ");
        for (ScmServiceInstance instance : serviceInstanceList) {
            totalCount++;
            logger.info("start check " + instance.getHostName() + ":" + instance.getPort()
                    + "(service=" + instance.getServiceName().toLowerCase() + ",ip="
                    + instance.getIp() + ") cluster connectivity");
            List<ScmCheckConnResult> connResults = null;
            try {
                connResults = ScmSystem.Diagnose.checkConnectivity(
                        instance.getIp() + ":" + instance.getPort(), target);
            }
            catch (ScmException e) {
                errorCount++;
                if (e.getErrorCode() == ScmError.NETWORK_IO.getErrorCode()) {
                    System.err.println(totalCount + "." + getPrintMsg(instance) + " DOWN");
                    logger.warn("check failed," + getPrintMsg(instance) + " is down");
                }
                else if (e.getErrorCode() == ScmError.HTTP_NOT_FOUND.getErrorCode()) {
                    System.err.println(totalCount + "." + getPrintMsg(instance) + ":"
                            + ScmError.HTTP_NOT_FOUND.getErrorCode() + " "
                            + ScmError.HTTP_NOT_FOUND.getErrorDescription());
                    logger.warn(getPrintMsg(instance) + ScmError.HTTP_NOT_FOUND.getErrorCode() + " "
                            + ScmError.HTTP_NOT_FOUND.getErrorDescription(), e);
                }
                else {
                    System.err.println(totalCount + "." + getPrintMsg(instance) + e.getError() + ","
                            + e.getMessage());
                    logger.error(e.getMessage(), e);
                }
                continue;
            }
            List<ScmCheckConnResult> failedResult = getFailedResult(connResults);
            if (failedResult.size() == 0) {
                System.out.println(totalCount + "." + getPrintMsg(instance) + " OK");
            }
            else {
                errorCount++;
                logger.warn("check failed," + getPrintMsg(instance)
                        + "failed to connect the following nodes：");
                System.err.println(totalCount + "." + getPrintMsg(instance)
                        + "failed to connect the following nodes：");
                for (ScmCheckConnResult result : failedResult) {
                    System.out.println(result.getHost() + ":" + result.getPort() + "(service="
                            + result.getService().toLowerCase() + ",ip=" + result.getIp() + ")");
                    logger.warn(result.getHost() + ":" + result.getPort() + "(service="
                            + result.getService().toLowerCase() + ",ip=" + result.getIp()
                            + ") check failed");
                }
                logger.info("");
            }
        }
        printCheckResult(totalCount, errorCount);
    }

    private String getPrintMsg(ScmServiceInstance instance) {
        return instance.getHostName() + ":" + instance.getPort() + "(service="
                + instance.getServiceName().toLowerCase() + ",ip=" + instance.getIp() + ")";
    }

    private String[] instanceListToNodeList(List<ScmServiceInstance> instanceList) {
        String[] nodeList = new String[instanceList.size()];
        int count = 0;
        for (ScmServiceInstance instance : instanceList) {
            nodeList[count++] = instance.getIp() + ":" + instance.getPort();
        }
        return nodeList;
    }

    private void printCheckResult(int totalCount, int errorCount) {
        System.out.println("total:" + totalCount + " success:" + (totalCount - errorCount)
                + " failed:" + errorCount);
    }

    private void printMultipleNetwork() {
        System.out.println("The nodes contains multiple network segments：");
        int count = 0;
        for (String key : ipCheckMap.keySet()) {
            if (count != 0) {
                System.out.println("");
            }
            System.out.println(++count + ". " + key + ".*.*");
            for (ScmServiceInstance instance : ipCheckMap.get(key)) {
                System.out.println(instance.getHostName() + ":" + instance.getPort() + "(service="
                        + instance.getServiceName().toLowerCase().toLowerCase() + ",ip:"
                        + instance.getIp() + ")");
            }
        }
        System.out.println("Please check the IP configuration of the hosts.");
    }

    private void printClusterInfoAndNetworkCheck(List<ScmServiceInstance> serviceInstanceList) {
        System.out.println("cluster info:");
        for (ScmServiceInstance instance : serviceInstanceList) {
            System.out.println(instance.getHostName() + ":" + instance.getPort() + "(service="
                    + instance.getServiceName().toLowerCase() + ",ip:" + instance.getIp() + ")");
            String[] ipSplit = instance.getIp().split("\\.");
            String ipPure = ipSplit[0] + "." + ipSplit[1];
            if (ipCheckMap.containsKey(ipPure)) {
                List<ScmServiceInstance> ipInstanceList = ipCheckMap.get(ipPure);
                ipInstanceList.add(instance);
            }
            else {
                List<ScmServiceInstance> ipInstanceList = new ArrayList<>();
                ipInstanceList.add(instance);
                ipCheckMap.put(ipPure, ipInstanceList);
            }
        }
        System.out.println();
    }

    private void changeLogbackFile(String logbackPath) throws ScmToolsException, IOException {
        InputStream is = null;
        try {
            if (StringUtils.isEmpty(logbackPath)) {
                is = new ClassPathResource("diagnoseLogback.xml").getInputStream();
            }
            else {
                is = new FileInputStream(logbackPath);
            }
            ScmHelper.configToolsLog(is);
        }
        catch (FileNotFoundException e) {
            throw new ScmToolsException(
                    "logbackPath is " + logbackPath + "The logback xml file does not exist",
                    LogCollectException.FILE_NOT_FIND);
        }
        finally {
            ScmCommon.closeResource(is);
        }
    }

    @Override
    protected Options addParam() throws ParseException {
        Options ops = new Options();
        ops.addOption(Option.builder("h").longOpt("help").hasArg(false).required(false).build());
        ops.addOption(Option.builder(null).longOpt(GATEWAY).desc("gateway url").optionalArg(true)
                .hasArg(true).required(true).build());
        ops.addOption(Option.builder(null).longOpt(LOGBACK_PATH).desc("logback path")
                .optionalArg(true).hasArg(true).required(false).build());
        return ops;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDesc() {
        return "execute scm cluster connCheck.";
    }

    protected void printHelp() throws ParseException {
        Options ops = addParam();
        HelpFormatter help = new HelpFormatter();
        help.printHelp(getName() + " [options]", ops);
    }
}
