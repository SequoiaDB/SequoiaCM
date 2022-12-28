package com.sequoiacm.deploy.rollbacker;

import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.core.ScmUpgradeStatusInfoMgr;
import com.sequoiacm.deploy.exception.UpgradeException;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallConfig;
import com.sequoiacm.deploy.module.NodeStatus;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.module.StatusInfo;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshExecRes;
import com.sequoiacm.deploy.ssh.SshMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceRollbackerBase implements ServiceRollbacker {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRollbackerBase.class);
    protected ServiceType type;
    protected SshMgr sshFactory = SshMgr.getInstance();
    protected ScmUpgradeStatusInfoMgr upgradeStatusInfoMgr = ScmUpgradeStatusInfoMgr.getInstance();
    protected CommonConfig commonConfig = CommonConfig.getInstance();

    public ServiceRollbackerBase(ServiceType type) {
        this.type = type;
    }

    @Override
    public void rollback(StatusInfo statusInfo) throws Exception {
        HostInfo host = upgradeStatusInfoMgr.getHostInfoWithCheck(statusInfo.getHostName());
        Ssh ssh = null;
        try {
            ssh = sshFactory.getSsh(host);
            try {
                rollback(ssh, statusInfo);
            } catch (Exception e) {
                logger.error("failed to rollback " + type + " on " + host.getHostName()
                        + ", causeby:" + e.getMessage());
            }
        } finally {
            CommonUtils.closeResource(ssh);
        }
    }

    @Override
    public ServiceType getType() {
        return type;
    }

    protected String getInstallPath() {
        return upgradeStatusInfoMgr.getInstallConfig().getInstallPath() + "/"
                + type.getInstllPack().getUntarDirName();
    }

    public void rollback(Ssh ssh, StatusInfo statusInfo) throws Exception {
        String remoteScriptPath = upgradeStatusInfoMgr.getConfigInfo().getUpgradePackPath() + "/"
                + new File(commonConfig.getLocalUpgradeScript()).getName();
        String command = remoteScriptPath + " --service " + type.getType() + " --install-path "
                + getInstallPath() + " --backup-path " + upgradeStatusInfoMgr.getConfigInfo().getBackupPath() + " --rollback";
        HostInfo hostInfo = upgradeStatusInfoMgr.getHostInfoWithCheck(ssh.getHost());
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("JAVA_HOME", hostInfo.getJavaHome());
        env.put("PATH", "$JAVA_HOME/bin:$PATH");
        SshExecRes sshExecRes = ssh.sudoSuExecRes(upgradeStatusInfoMgr.getInstallConfig().getInstallUser(), command, env);
        if (sshExecRes.getExitCode() == 1) {
            throw new UpgradeException("failed to stop node, please check remote log:(" + upgradeStatusInfoMgr.getInstallConfig().getInstallPath()
                    + "/log, host=" + ssh.getHost() + ", service=" + getType() + ")");
        }
        if (sshExecRes.getExitCode() == 2) {
            throw new UpgradeException(sshExecRes.getStdOut());
        }
        List<NodeStatus> nodeStatusList = statusInfo.getNodeStatus();
        if (!nodeStatusList.isEmpty()) {
            try {
                startNode(ssh, nodeStatusList, env);
            } catch (Exception e) {
                throw new UpgradeException("failed to start node, please check remote log:(" + upgradeStatusInfoMgr.getInstallConfig().getInstallPath()
                        + "/log, host=" + ssh.getHost() + ", service=" + getType() + ")", e);
            }
        }
    }

    protected void startNode(Ssh ssh, List<NodeStatus> nodeStatusList, Map<String, String> env)
            throws Exception {
        InstallConfig installConfig = upgradeStatusInfoMgr.getInstallConfig();
        for (NodeStatus nodeStatus : nodeStatusList) {
            if (nodeStatus.isStart()) {
                logger.info("Restarting " + type + " node:port=" + nodeStatus.getPort());
                String startCmd = getInstallPath() + "/bin/" + type.getStartStopScript()
                        + " start --timeout " + CommonUtils.getWaitServiceReadyTimeout() + " -p " + nodeStatus.getPort();
                ssh.sudoSuExecRes(installConfig.getInstallUser(), startCmd, env);
            }
        }
    }
}
