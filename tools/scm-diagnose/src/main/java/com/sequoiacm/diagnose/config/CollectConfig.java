package com.sequoiacm.diagnose.config;

import com.sequoiacm.diagnose.common.Services;
import com.sequoiacm.diagnose.ssh.Ssh;
import com.sequoiacm.diagnose.utils.AnalyzeConfUtils;
import com.sequoiacm.infrastructure.tool.common.ScmCommon;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.cli.CommandLine;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectConfig {
    private static String installPath = "/opt/sequoiacm";
    private static String outputPath;
    private static List<String> serviceList = new ArrayList<>();
    private static String resultDir = "scm-collect-logs";
    private static int maxLogCount = 1;
    private static int threadSize = 1;
    private static boolean needZip = true;

    private static final String HOSTS = "hosts";
    private static final String CONF = "conf";
    private static final String INSTALL_PATH = "install-path";
    private static final String OUTPUT_PATH = "output-path";
    private static final String SHORT_OUTPUT_PATH = "o";
    private static final String SERVICES = "services";
    private static final String MAX_LOG_COUNT = "max-log-count";
    private static final String THREAD = "thread-size";
    private static final String NEED_ZIP = "need-zip";
    private static Map<String, String> serviceMap = new HashMap();

    static {
        try {
            initService();
            initServerMap();
        }
        catch (ScmToolsException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init(CommandLine commandLine, List<Ssh> sshList)
            throws IOException, ScmToolsException {
        // analyzing conf
        Map<String, List<String>> confMap = null;
        if (commandLine.hasOption(CONF)) {
            String confPath = commandLine.getOptionValue(CONF);
            confMap = AnalyzeConfUtils.analyzeConfFile(confPath);
            if (confMap != null && confMap.get("collectConfig") != null) {
                AnalyzeConfUtils.analyzeCollectConfig(confMap.get("collectConfig"));
            }
        }

        // analyzing hosts
        if (commandLine.hasOption(HOSTS)) {
            String hosts = commandLine.getOptionValue(HOSTS);
            AnalyzeConfUtils.analyzeHostInfoByHost(sshList, hosts);
        }
        // analyzing hosts in conf
        else {
            if (confMap != null) {
                List<String> hostList = confMap.get(HOSTS);
                if (hostList != null) {
                    AnalyzeConfUtils.analyzeHostInfoByConf(sshList, hostList);
                }
            }
        }

        if (commandLine.hasOption(INSTALL_PATH)) {
            installPath = commandLine.getOptionValue(INSTALL_PATH);
        }

        analyzeOutputPath(commandLine);
        analyzeServices(commandLine);

        if (commandLine.hasOption(MAX_LOG_COUNT)) {
            maxLogCount = Integer.parseInt(commandLine.getOptionValue(MAX_LOG_COUNT));
            if (maxLogCount < -1) {
                throw new IllegalArgumentException("Invalid arg " + MAX_LOG_COUNT + "="
                        + maxLogCount + ",it must greater than -1");
            }
        }

        if (commandLine.hasOption(THREAD)) {
            threadSize = Integer.parseInt(commandLine.getOptionValue(THREAD));
            if (threadSize <= 0) {
                throw new IllegalArgumentException("Invalid arg " + THREAD + "=" + threadSize
                        + ",it must be a positive integer");
            }
        }

        if (commandLine.hasOption(NEED_ZIP)) {
            String value = commandLine.getOptionValue(NEED_ZIP);
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                needZip = Boolean.parseBoolean(value);
            }
            else {
                throw new IllegalArgumentException(
                        "Invalid arg " + NEED_ZIP + "=" + value + ",it must be boolean type");
            }
        }
    }

    public static void assignmentCollectConfig(String key, String value) {
        switch (key) {
            case SERVICES:
                serviceList = new ArrayList<>();
                for (String service : value.split(",")) {
                    if (CollectConfig.getServerMap().get(service) != null) {
                        serviceList.add(service);
                    }
                    else {
                        throw new IllegalArgumentException(
                                "collectConfig illegal configuration," + SERVICES + "=" + value
                                        + "," + service + " is not exist in scm services");
                    }
                }
                break;
            case MAX_LOG_COUNT:
                try {
                    maxLogCount = Integer.parseInt(value);
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("collectConfig illegal configuration, "
                            + MAX_LOG_COUNT + "=" + value + ", it must be a number");
                }
                if (maxLogCount < -1) {
                    throw new IllegalArgumentException("collectConfig illegal configuration, "
                            + MAX_LOG_COUNT + "=" + value + ", it must >= -1");
                }
                break;
            case OUTPUT_PATH:
                outputPath = value;
                break;
            case INSTALL_PATH:
                installPath = value;
                break;
            case THREAD:
                try {
                    threadSize = Integer.parseInt(value);
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("collectConfig illegal configuration, "
                            + THREAD + "=" + value + ", it must be a number");
                }
                if (threadSize < 1) {
                    throw new IllegalArgumentException("collectConfig illegal configuration, "
                            + THREAD + "=" + value + ", it must > 0");
                }
                break;
            case NEED_ZIP:
                if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                    needZip = Boolean.parseBoolean(value);
                    break;
                }
                else {
                    throw new IllegalArgumentException("collectConfig illegal configuration, "
                            + NEED_ZIP + "=" + value + ", it must be boolean type");
                }
            default:
                SshCommonConfig.assignmentCollectConfig(key, value);
                break;
        }
    }

    private static void initServerMap() {
        for (Services service : Services.values()) {
            serviceMap.put(service.getServiceName(), service.getServiceInstallPath());
        }
    }

    public static void initService() throws ScmToolsException {
        for (Services service : Services.values()) {
            serviceList.add(service.getServiceName());
        }
    }

    private static void analyzeServices(CommandLine commandLine) {
        if (commandLine.hasOption(SERVICES)) {
            serviceList = new ArrayList<>();
            String services = commandLine.getOptionValue(SERVICES);
            for (String service : services.split(",")) {
                if (StringUtils.isEmpty(CollectConfig.getServerMap().get(service))) {
                    throw new IllegalArgumentException(
                            "services is invalid arg " + service + " is not exist in scm services");
                }
                serviceList.add(service);
            }
        }
    }

    private static void analyzeOutputPath(CommandLine commandLine) throws ScmToolsException {
        if (commandLine.hasOption(SHORT_OUTPUT_PATH)) {
            outputPath = commandLine.getOptionValue(SHORT_OUTPUT_PATH);
        }
        else {
            if (StringUtils.isEmpty(CollectConfig.getOutputPath())) {
                outputPath = ScmCommon.getUserWorkingDir();
            }
        }
    }

    public static String getInstallPath() {
        return installPath;
    }

    public static String getOutputPath() {
        return outputPath;
    }

    public static List<String> getServiceList() {
        return serviceList;
    }

    public static String getResultDir() {
        return resultDir;
    }

    public static void setResultDir(String resultDir) {
        CollectConfig.resultDir = resultDir;
    }

    public static int getMaxLogCount() {
        return maxLogCount;
    }

    public static int getThreadSize() {
        return threadSize;
    }

    public static boolean isNeedZipCopy() {
        return needZip;
    }

    public static Map<String, String> getServerMap() {
        return serviceMap;
    }

}
