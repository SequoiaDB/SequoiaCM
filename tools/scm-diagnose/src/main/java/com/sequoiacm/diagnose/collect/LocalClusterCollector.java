package com.sequoiacm.diagnose.collect;

import com.sequoiacm.diagnose.command.ScmClusterCollect;
import com.sequoiacm.diagnose.common.ClusterCommand;
import com.sequoiacm.diagnose.common.CollectResult;
import com.sequoiacm.diagnose.common.ScmNodeInfo;
import com.sequoiacm.diagnose.config.CollectConfig;
import com.sequoiacm.diagnose.execption.CollectException;
import com.sequoiacm.diagnose.utils.ExecLinuxCommandUtils;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

public class LocalClusterCollector extends ClusterCollector {

    private String hostName = "localhost";
    private StringBuilder pidBuild = new StringBuilder();
    private List<ScmNodeInfo> nodeInfoList;
    static final Logger logger = LoggerFactory.getLogger(LocalClusterCollector.class);

    public LocalClusterCollector(List<ScmNodeInfo> nodeInfoList) {
        this.nodeInfoList = nodeInfoList;
    }

    @Override
    public CollectResult call() throws Exception {
        try {
            this.start();
        }
        catch (Exception e) {
            return new CollectResult(-1, "local host collect cluster info failed," + e.getMessage(),
                    e);
        }
        return new CollectResult(0, "local host collect cluster info successful");
    }

    @Override
    public void start() throws IOException, ScmToolsException {
        System.out.println("[INFO ] local host start cluster info collect");
        hostName = InetAddress.getLocalHost().getHostName();
        String hostPath = CollectConfig.getOutputPath() + File.separator
                + ScmClusterCollect.currentCollectPath + File.separator + hostName;
        String hostInfoOutPath = hostPath + File.separator + "host_info";
        String nodesOutPath = hostPath + File.separator + "node_info";
        getHostInfo(hostInfoOutPath);
        getNodesInfo(nodesOutPath);

        String topAllPidOutput = hostInfoOutPath + File.separator + "top_allPid.txt";
        String topAllPidInfoCmd = ClusterCommand.getTopAllPidInfoCmd(pidBuild.toString(),
                topAllPidOutput);
        try {
            ExecLinuxCommandUtils.localExecuteCommand(topAllPidInfoCmd);
        }
        catch (ScmToolsException e) {
            System.out.println("[WARN ] collect local host top all scm node pid info failed");
            logger.warn("collect local host top all scm node pid info failed," + e.getMessage(), e);
            dealWithError(e, topAllPidOutput);
        }
        if (CollectConfig.isNeedZipCopy()) {
            String tarName = hostPath + File.separator + hostName + ".tar.gz";
            ExecLinuxCommandUtils.zipFile(tarName, hostPath, Arrays
                    .asList(new File(hostInfoOutPath).getName(), new File(nodesOutPath).getName()));
            FileUtils.deleteDirectory(new File(hostInfoOutPath));
            FileUtils.deleteDirectory(new File(nodesOutPath));
        }
        System.out.println("[INFO ] local host cluster info collect finished");
    }

    private void getNodesInfo(String nodesOutputPath) throws IOException, ScmToolsException {

        for (ScmNodeInfo nodeInfo : nodeInfoList) {
            String nodeInfoOutputPath = nodesOutputPath + File.separator + nodeInfo.getServiceName()
                    + "_" + nodeInfo.getPort();
            FileUtils.forceMkdir(new File(nodeInfoOutputPath));

            logger.info("local get node pid by port,node is " + nodeInfo.getIp_addr() + ":"
                    + nodeInfo.getPort());
            String outputFile = nodeInfoOutputPath + File.separator + "jstack.txt";
            int pid;
            try {
                pid = getNodePid(nodeInfo);
            }
            catch (ScmToolsException e) {
                System.out.println("[WARN ] local get node pid by port failed,node is "
                        + nodeInfo.getIp_addr() + ":" + nodeInfo.getPort());
                logger.warn("local get node pid by port failed,node is " + nodeInfo.getIp_addr()
                        + ":" + nodeInfo.getPort() + "," + e.getMessage(), e);
                dealWithError(e, outputFile);
                continue;
            }
            nodeInfo.setPid(pid);
            pidBuild.append(" -p " + pid);

            logger.info("collect local node jstack by pid,node is " + nodeInfo.getIp_addr() + ":"
                    + nodeInfo.getPort());
            String jstackCmd = ClusterCommand.getNodeJstackCmd(pid,
                    outputFile);
            try {
                ExecLinuxCommandUtils.localExecuteCommand(jstackCmd);
            }
            catch (ScmToolsException e) {
                System.out.println("[WARN ] collect local node jstack by pid failed, node is "
                        + nodeInfo.getIp_addr() + ":" + nodeInfo.getPort());
                logger.warn("collect local node jstack by pid failed, node is "
                        + nodeInfo.getIp_addr() + ":" + nodeInfo.getPort() + "," + e.getMessage(),
                        e);
                dealWithError(e, outputFile);
            }

            logger.info("collect local node " + nodeInfo.getIp_addr() + ":" + nodeInfo.getPort()
                    + " tcp info");
            outputFile = nodeInfoOutputPath + File.separator + "tcp.txt";
            String nodeTcpCmd = ClusterCommand.getNodeTcpCmd(nodeInfo.getPort(), outputFile);
            try {
                ExecLinuxCommandUtils.localExecuteCommand(nodeTcpCmd);
            }
            catch (ScmToolsException e) {
                System.out.println("[WARN ] collect local node " + nodeInfo.getIp_addr() + ":"
                        + nodeInfo.getPort() + " tcp info failed");
                logger.warn("collect local node " + nodeInfo.getIp_addr() + ":" + nodeInfo.getPort()
                        + " tcp info failed," + e.getMessage(), e);
                dealWithError(e, outputFile);
            }
        }
    }

    private int getNodePid(ScmNodeInfo nodeInfo) throws ScmToolsException {
        String jarNamePrefix;
        if (nodeInfo.isContentServer()) {
            jarNamePrefix = ScmNodeTypeEnum.CONTENTSERVER.getJarNamePrefix();
        }
        else if (nodeInfo.isS3Server()) {
            jarNamePrefix = ScmNodeTypeEnum.S3SERVER.getJarNamePrefix();
        }
        else {
            ScmNodeTypeEnum scmNode = ScmNodeTypeEnum.getScmNodeByName(nodeInfo.getServiceName());
            jarNamePrefix = scmNode.getJarNamePrefix();
        }
        String command = ClusterCommand.getNodePidMsgCmd(jarNamePrefix);
        String servicePidMsgs = ExecLinuxCommandUtils.localExecuteCommand(command).getStdOut()
                .trim();
        int pid = -1;
        for (String pidMsg : servicePidMsgs.split("\n")) {
            if (pidMsg.contains("/" + nodeInfo.getPort())) {
                pid = ScmCommandUtil.getPidFromPsResult(pidMsg);
                break;
            }
        }
        if (pid == -1) {
            throw new ScmToolsException(
                    "local node get pid failed,node is " + nodeInfo.getIp_addr() + ":"
                            + nodeInfo.getPort() + ",get pid command=" + command,
                    CollectException.GET_PID_FAILED);
        }
        else {
            return pid;
        }
    }
    private void getHostInfo(String hostInfoOutPath) throws IOException, ScmToolsException {

        FileUtils.forceMkdir(new File(hostInfoOutPath));

        logger.info("collect local host memory info");
        String outputFile = hostInfoOutPath + File.separator + "memory.txt";
        String memoryCmd = ClusterCommand
                .getMemoryInfoCmd(outputFile);
        try {
            ExecLinuxCommandUtils.localExecuteCommand(memoryCmd);
        }
        catch (ScmToolsException e) {
            System.out.println("[WARN ] collect local host memory info failed");
            logger.warn("collect local host memory info failed," + e.getMessage(), e);
            dealWithError(e, outputFile);
        }

        logger.info("collect local host cpu info");
        outputFile = hostInfoOutPath + File.separator + "cpu.txt";
        String cpuCmd = ClusterCommand.getCpuInfoCmd(outputFile);
        try {
            ExecLinuxCommandUtils.localExecuteCommand(cpuCmd);
        }
        catch (ScmToolsException e) {
            System.out.println("[WARN ] collect local host cpu info failed");
            logger.warn("collect local host cpu info failed," + e.getMessage(), e);
            dealWithError(e, outputFile);
        }

        logger.info("collect local host system info");
        outputFile = hostInfoOutPath + File.separator + "system.txt";
        String systemCmd = ClusterCommand
                .getSystemInfoCmd(outputFile);
        try {
            ExecLinuxCommandUtils.localExecuteCommand(systemCmd);
        }
        catch (ScmToolsException e) {
            System.out.println("[WARN ] collect local host System info failed");
            logger.warn("collect local host System info failed," + e.getMessage(), e);
            dealWithError(e, outputFile);
        }

        logger.info("collect local host disk info");
        outputFile = hostInfoOutPath + File.separator + "disk.txt";
        String diskCmd = ClusterCommand
                .getDiskInfoCmd(outputFile);
        try {
            ExecLinuxCommandUtils.localExecuteCommand(diskCmd);
        }
        catch (ScmToolsException e) {
            System.out.println("[WARN ] collect local host disk info failed");
            logger.warn("collect local host disk info failed," + e.getMessage(), e);
            dealWithError(e, outputFile);
        }

        logger.info("collect local host ifconfig info");
        outputFile = hostInfoOutPath + File.separator + "ifconfig.txt";
        String ifconfigCmd = ClusterCommand
                .getIfconfigInfoCmd(outputFile);
        try {
            ExecLinuxCommandUtils.localExecuteCommand(ifconfigCmd);
        }
        catch (ScmToolsException e) {
            System.out.println("[WARN ] collect local host ifconfig info failed");
            logger.warn("collect local host ifconfig info failed," + e.getMessage(), e);
            dealWithError(e, outputFile);
        }

        logger.info("collect local host top all info");
        outputFile = hostInfoOutPath + File.separator + "top_all.txt";
        String topAllCmd = ClusterCommand
                .getTopAllInfoCmd(outputFile);
        try {
            ExecLinuxCommandUtils.localExecuteCommand(topAllCmd);
        }
        catch (ScmToolsException e) {
            System.out.println("[WARN ] collect local host top all info failed");
            logger.warn(" collect local host top all info failed," + e.getMessage(), e);
            dealWithError(e, outputFile);
        }
    }

    private void dealWithError(Exception e, String outputPath)
            throws IOException, ScmToolsException {
        String exceptionStack = CollectException.getExceptionStack(e);
        String errorCmd = "echo \" " + exceptionStack + "\" >> " + outputPath;
        ExecLinuxCommandUtils.localExecuteCommand(errorCmd);
    }
}
