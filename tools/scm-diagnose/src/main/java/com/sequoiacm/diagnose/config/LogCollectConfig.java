package com.sequoiacm.diagnose.config;

import com.sequoiacm.diagnose.common.Services;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LogCollectConfig {
    private static String installPath = "/opt/sequoiacm";
    private static String outputPath = null;
    private static List<String> serviceList = new ArrayList<>();
    private static String resultDir = "scm-collect-logs";
    private static int maxLogCount = -1;
    private static int threadSize = 1;
    private static boolean needZipCopy = true;

    public static HashMap<String, String> serviceMap = new HashMap();

    static {
        try {
            initService();
            initServerMap();
        }
        catch (ScmToolsException e) {
            throw new RuntimeException(e);
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

    public static String getInstallPath() {
        return installPath;
    }

    public static void setInstallPath(String installPath) {
        LogCollectConfig.installPath = installPath;
    }

    public static String getOutputPath() {
        return outputPath;
    }

    public static void setOutputPath(String outputPath) {
        LogCollectConfig.outputPath = outputPath;
    }

    public static List<String> getServiceList() {
        return serviceList;
    }

    public static void setServiceList(List<String> serviceList) {
        LogCollectConfig.serviceList = serviceList;
    }

    public static String getResultDir() {
        return resultDir;
    }

    public static void setResultDir(String resultDir) {
        LogCollectConfig.resultDir = resultDir;
    }

    public static int getMaxLogCount() {
        return maxLogCount;
    }

    public static void setMaxLogCount(int maxLogCount) {
        LogCollectConfig.maxLogCount = maxLogCount;
    }

    public static int getThreadSize() {
        return threadSize;
    }

    public static void setThreadSize(int threadSize) {
        LogCollectConfig.threadSize = threadSize;
    }

    public static boolean isNeedZipCopy() {
        return needZipCopy;
    }

    public static void setNeedZipCopy(boolean needZipCopy) {
        LogCollectConfig.needZipCopy = needZipCopy;
    }

    public static HashMap<String, String> getServerMap() {
        return serviceMap;
    }

}
