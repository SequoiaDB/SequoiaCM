package com.sequoiacm.diagnose.collect;

import com.sequoiacm.diagnose.command.ScmClusterCollect;
import com.sequoiacm.diagnose.common.ClusterCommand;
import com.sequoiacm.diagnose.common.CollectResult;
import com.sequoiacm.diagnose.common.ScmNodeInfo;
import com.sequoiacm.diagnose.config.CollectConfig;
import com.sequoiacm.diagnose.execption.CollectException;
import com.sequoiacm.diagnose.ssh.Ssh;
import com.sequoiacm.diagnose.utils.ExecLinuxCommandUtils;
import com.sequoiacm.infrastructure.tool.common.ScmCommandUtil;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class RemoteClusterCollector extends ClusterCollector {

    private Ssh ssh;
    private final String TEMP_PATH = "/tmp";
    private List<ScmNodeInfo> nodeInfoList;
    private StringBuilder pidBuild = new StringBuilder();
    private static final Logger logger = LoggerFactory.getLogger(RemoteClusterCollector.class);

    public RemoteClusterCollector(List<ScmNodeInfo> nodeInfoList) {
        this.nodeInfoList = nodeInfoList;
    }

    public Ssh getSsh() {
        return ssh;
    }

    public void setSsh(Ssh ssh) {
        this.ssh = ssh;
    }

    @Override
    public CollectResult call() throws Exception {
        try {
            this.start();
            return new CollectResult(0,
                    "remote host " + ssh.getHost() + " collect cluster info successful");
        }
        catch (Exception e) {
            return new CollectResult(-1, "remote host " + ssh.getHost()
                    + " collect cluster info failed:" + e.getMessage(), e);
        }
    }

    @Override
    public void start() throws ScmToolsException, IOException {
        System.out.println(
                "[INFO ] remote host " + getSsh().getHost() + " start cluster info collect");
        String tempCollectPath = TEMP_PATH + File.separator + CollectConfig.getResultDir();
        try {
            ssh.rmDir(tempCollectPath);
            ssh.mkdir(tempCollectPath);
            remoteCollectClusterFile(tempCollectPath);
            ssh.rmDir(tempCollectPath + "*");
        }
        catch (ScmToolsException e) {
            throw e;
        }
        finally {
            ssh.disconnect();
        }
        System.out.println(
                "[INFO ] remote host " + getSsh().getHost() + " cluster info collect finished");
    }

    private void remoteCollectClusterFile(String outputPath) throws ScmToolsException, IOException {
        String hostInfoOutPath = outputPath + File.separator + "host_info";
        String nodesOutPath = outputPath + File.separator + "node_info";
        getHostInfo(hostInfoOutPath);
        getNodesInfo(nodesOutPath);

        logger.info("remote host " + ssh.getHost() + " collect all scm node pid info");
        String allPidOutputPath = hostInfoOutPath + File.separator + "top_allPid.txt";
        getTopAllPidInfo(allPidOutputPath);
        String tarName = outputPath + File.separator + ssh.getHost() + ".tar.gz";
        ssh.zipFile(tarName, outputPath, Arrays.asList(new File(hostInfoOutPath).getName(),
                new File(nodesOutPath).getName()));
        String dest = CollectConfig.getOutputPath() + File.separator
                + ScmClusterCollect.currentCollectPath + File.separator + ssh.getHost()
                + File.separator + ssh.getHost() + ".tar.gz";
        FileUtils.forceMkdirParent(new File(dest));
        ssh.copyFileFromRemote(tarName, dest);
        if (!CollectConfig.isNeedZipCopy()) {
            boolean result = ExecLinuxCommandUtils.localUnzipNodeTar(dest,
                    new File(dest).getParentFile().getAbsolutePath());
            if (result) {
                File tarFile = new File(dest);
                if (!tarFile.delete()) {
                    logger.warn("file delete failed,path=" + tarFile.getAbsolutePath(),
                            CollectException.FILE_NOT_FIND);
                }
            }
        }
    }

    private void getTopAllPidInfo(String outputPath) throws IOException, ScmToolsException {
        String command = ClusterCommand.getTopAllPidInfoHasArgWCmd(pidBuild.toString(), outputPath);
        try {
            ssh.sshExecuteCommand(command);
        }
        catch (ScmToolsException e) {
            if (e.getExitCode() == CollectException.SHELL_EXEC_ERROR) {
                command = ClusterCommand.getTopAllPidInfoNoArgWCmd(pidBuild.toString(), outputPath);
                try {
                    ssh.sshExecuteCommand(command);
                }
                catch (ScmToolsException ex) {
                    logger.warn("remote host " + ssh.getHost()
                            + " collect top all scm node pid info failed", e);
                    String consoleMsg = "remote host " + ssh.getHost()
                            + " collect top all scm node pid info failed";
                    dealWithError(consoleMsg, ex, outputPath);
                }
            }
            else {
                String consoleMsg = "remote host " + ssh.getHost()
                        + " collect top all scm node pid info failed";
                dealWithError(consoleMsg, e, outputPath);
            }
        }
    }

    private void getHostInfo(String hostInfoOutPath) throws ScmToolsException, IOException {
        ssh.mkdir(hostInfoOutPath);

        logger.info("remote host " + ssh.getHost() + " collect memory info");
        String outputPath = hostInfoOutPath + File.separator + "memory.txt";
        String command = ClusterCommand.getMemoryInfoCmd(outputPath);
        try {
            ssh.sshExecuteCommand(command);
        }
        catch (ScmToolsException e) {
            String consoleMsg = "remote host " + ssh.getHost() + " collect memory info failed";
            dealWithError(consoleMsg, e, outputPath);
        }

        logger.info("remote host " + ssh.getHost() + " collect cpu info");
        outputPath = hostInfoOutPath + File.separator + "cpu.txt";
        command = ClusterCommand.getCpuInfoCmd(outputPath);
        try {
            ssh.sshExecuteCommand(command);
        }
        catch (ScmToolsException e) {
            String consoleMsg = "remote host " + ssh.getHost() + " collect cpu info failed";
            dealWithError(consoleMsg, e, outputPath);
        }

        logger.info("remote host " + ssh.getHost() + " collect system info");
        outputPath = hostInfoOutPath + File.separator + "system.txt";
        command = ClusterCommand.getSystemInfoCmd(outputPath);
        try {
            ssh.sshExecuteCommand(command);
        }
        catch (ScmToolsException e) {
            String consoleMsg = "remote host " + ssh.getHost() + " collect system info failed";
            dealWithError(consoleMsg, e, outputPath);
        }

        logger.info("remote host " + ssh.getHost() + " collect disk info");
        outputPath = hostInfoOutPath + File.separator + "disk.txt";
        command = ClusterCommand.getDiskInfoCmd(outputPath);
        try {
            ssh.sshExecuteCommand(command);
        }
        catch (ScmToolsException e) {
            String consoleMsg = "remote host " + ssh.getHost() + " collect disk info failed";
            dealWithError(consoleMsg, e, outputPath);
        }

        logger.info("remote host " + ssh.getHost() + " collect ifconfig info");
        outputPath = hostInfoOutPath + File.separator + "ifconfig.txt";
        command = ClusterCommand.getIfconfigInfoCmd(outputPath);
        try {
            ssh.sshExecuteCommand(command);
        }
        catch (ScmToolsException e) {
            String consoleMsg = "remote collect host " + ssh.getHost()
                    + " collect ifconfig info failed";
            dealWithError(consoleMsg, e, outputPath);
        }

        logger.info("remote host " + ssh.getHost() + " collect top all info");
        outputPath = hostInfoOutPath + File.separator + "top_all.txt";
        getTopAllInfo(outputPath);
    }

    private void getTopAllInfo(String outputPath) throws IOException, ScmToolsException {
        String command = ClusterCommand.getTopAllInfoHasArgWCmd(outputPath);
        try {
            ssh.sshExecuteCommand(command);
        }
        catch (ScmToolsException e) {
            if (e.getExitCode() == CollectException.SHELL_EXEC_ERROR) {
                command = ClusterCommand.getTopAllInfoNoArgWCmd(outputPath);
                try {
                    ssh.sshExecuteCommand(command);
                }
                catch (ScmToolsException ex) {
                    logger.warn(
                            "remote collect host " + ssh.getHost() + " collect top all info failed",
                            e);
                    String consoleMsg = "remote collect host " + ssh.getHost()
                            + " collect top all info failed";
                    dealWithError(consoleMsg, ex, outputPath);
                }
            }
            else {
                String consoleMsg = "remote collect host " + ssh.getHost()
                        + " collect top all info failed";
                dealWithError(consoleMsg, e, outputPath);
            }
        }
    }

    private void getNodesInfo(String nodesOutputPath) throws ScmToolsException, IOException {

        for (ScmNodeInfo nodeInfo : nodeInfoList) {
            String nodeInfoOutPath = nodesOutputPath + File.separator + nodeInfo.getServiceName()
                    + "_"
                    + nodeInfo.getPort();
            ssh.mkdir(nodeInfoOutPath);

            logger.info("remote host get " + ssh.getHost() + ":" + nodeInfo.getPort() + " pid");
            String outputPath = nodeInfoOutPath + File.separator + "jstack.txt";
            int pid;
            try {
                pid = getNodePid(nodeInfo);
            }
            catch (ScmToolsException e) {
                String consoleMsg = "remote host " + ssh.getHost()
                        + " get node pid by port failed,node is " + ssh.getHost() + ":"
                        + nodeInfo.getPort();
                dealWithError(consoleMsg, e, outputPath);
                continue;
            }
            nodeInfo.setPid(pid);
            pidBuild.append(" -p " + pid);

            logger.info("remote host collect " + ssh.getHost() + ":" + nodeInfo.getPort()
                    + " jstack info");
            String command = ClusterCommand.getNodeJstackCmd(pid, outputPath);
            try {
                ssh.sshExecuteCommand(command);
            }
            catch (ScmToolsException e) {
                String consoleMsg = "remote host collect " + ssh.getHost() + ":"
                        + nodeInfo.getPort() + " jstack info failed";
                dealWithError(consoleMsg, e, outputPath);
            }

            logger.info("remote host collect " + ssh.getHost() + ":" + nodeInfo.getPort()
                    + " tcp info");
            outputPath = nodeInfoOutPath + File.separator + "tcp.txt";
            command = ClusterCommand.getNodeTcpCmd(nodeInfo.getPort(), outputPath);
            try {
                ssh.sshExecuteCommand(command);
            }
            catch (ScmToolsException e) {
                String consoleMsg = "remote host collect " + ssh.getHost() + ":"
                        + nodeInfo.getPort() + " tcp info failed";
                dealWithError(consoleMsg, e, outputPath);
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
        String servicePidMsgs = ssh.sshExecuteCommand(command).getStdOut().trim();
        int pid = -1;
        for (String pidMsg : servicePidMsgs.split("\n")) {
            if (pidMsg.contains("/" + nodeInfo.getPort())) {
                pid = ScmCommandUtil.getPidFromPsResult(pidMsg);
                break;
            }
        }
        if (pid == -1) {
            throw new ScmToolsException(
                    "remote host " + ssh.getHost() + " node get pid failed,node is " + ssh.getHost()
                            + ":" + nodeInfo.getPort() + ",get pid command=" + command,
                    CollectException.GET_PID_FAILED);
        }
        else {
            return pid;
        }
    }

    private void dealWithError(String consoleMsg, Exception e, String outputPath)
            throws IOException, ScmToolsException {
        System.err.println("[WARN ] " + consoleMsg);
        logger.warn(consoleMsg + "," + e.getMessage(), e);
        String exceptionStack = CollectException.getExceptionStack(e);
        String errorCmd = "echo \"" + exceptionStack + "\" >> " + outputPath;
        ssh.sshExecuteCommand(errorCmd);
    }

}
