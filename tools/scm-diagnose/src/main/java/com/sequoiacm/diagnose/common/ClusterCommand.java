package com.sequoiacm.diagnose.common;

import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;

public class ClusterCommand {
    public static String getMemoryInfoCmd(String outputPath) {
        return "free -h > " + outputPath;
    }

    public static String getCpuInfoCmd(String outputPath) {
        return "cat /proc/cpuinfo > " + outputPath;
    }

    public static String getSystemInfoCmd(String outputPath) {
        return "uname -a > " + outputPath;
    }

    public static String getDiskInfoCmd(String outputPath) {
        return "df -h > " + outputPath;
    }

    public static String getIfconfigInfoCmd(String outputPath) {
        return "ifconfig > " + outputPath;
    }

    public static String getTopAllInfoNoArgWCmd(String outputPath) {
        return "top -b -n 3 -d 1 -c > " + outputPath;
    }

    public static String getTopAllInfoHasArgWCmd(String outputPath) {
        return "top -b -n 3 -d 1 -c -w 512 > " + outputPath;
    }

    public static String getNodeJstackCmd(int pid, String outputPath) {
        return "source /etc/profile;jstack -F " + pid + " > " + outputPath;
    }

    public static String getNodeTcpCmd(int port, String outputPath) {
        return "netstat -nat | grep " + port + " > " + outputPath;
    }

    public static String getNodePidMsgCmd(String jarNamePrefix) {
        return ScmCommandUtil.getPidCommandByjarName(jarNamePrefix);
    }

    public static String getTopAllPidInfoNoArgWCmd(String allPid, String outputPath) {
        return "top -b -n 3 -d 1 -c " + allPid + " > " + outputPath;
    }

    public static String getTopAllPidInfoHasArgWCmd(String allPid, String outputPath) {
        return "top -b -n 3 -d 1 -c -w 512 " + allPid + " > " + outputPath;
    }
}
