package com.sequoiacm.deploy.upgrader;

import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.config.CommonConfig;
import com.sequoiacm.deploy.core.ScmUpgradeInfoMgr;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceUpgraderBase implements ServiceUpgrader {

    private static final Logger logger = LoggerFactory.getLogger(ServiceUpgraderBase.class);
    protected SshMgr sshFactory = SshMgr.getInstance();
    protected ScmUpgradeInfoMgr upgradeInfoMgr = ScmUpgradeInfoMgr.getInstance();
    protected CommonConfig commonConfig = CommonConfig.getInstance();
    protected ServiceType type;

    public ServiceUpgraderBase(ServiceType type) {
        this.type = type;
    }

    @Override
    public ServiceType getType() {
        return type;
    }

    @Override
    public void upgrade(StatusInfo statusInfo) throws Exception {
        HostInfo host = upgradeInfoMgr.getHostInfoWithCheck(statusInfo.getHostName());
        List<NodeStatus> nodeStatusList = statusInfo.getNodeStatus();
        Ssh ssh = null;
        try {
            ssh = sshFactory.getSsh(host);
            String upgradeScriptPath = upgradeInfoMgr.getConfigInfo().getUpgradePackPath() + "/"
                    + new File(commonConfig.getLocalUpgradeScript()).getName();
            // scmupgrade.py --service gateway --install-path /opt/sequoiacm/sequoiacm-cloud --backup-path /opt/upgrade/backup/sequoiacm-cloud
            String upgradeCommand = upgradeScriptPath + " --service " + type.getType() + " --install-path "
                    + getInstallPath() + " --backup-path " + statusInfo.getBackupPath();
            try {
                execScript(ssh, upgradeCommand, nodeStatusList);
            } catch (Exception e) {
                // 当升级过程中抛出异常, 执行回滚
                logger.error("failed to upgrade " + type + " on " + host.getHostName() + ", ready to rollback " + type + " on " + host.getHostName());
                logger.info("Rollbacking Service: rollbacking {} on {}", type, ssh.getHost());
                String rollbackCommand = upgradeCommand + " --rollback";
                try {
                    execScript(ssh, rollbackCommand, nodeStatusList);
                    logger.info("Rollback success");
                } catch (Exception e1) {
                    logger.error("failed to rollback " + type + " on " + host.getHostName() + ", causeby:" + e1.getMessage(), e1);
                }
                throw new UpgradeException("failed to upgrade " + type + " on " + host.getHostName() + ", causeby:" + e.getMessage(), e);
            }
        } finally {
            CommonUtils.closeResource(ssh);
        }
    }

    @Override
    public List<NodeStatus> getNodeStatus(HostInfo host) throws Exception {
        String startStopScript = getInstallPath() + "/bin/" + type.getStartStopScript();
        List<NodeStatus> nodeStatusList = new ArrayList<>();
        Ssh ssh = null;
        try {
            ssh = sshFactory.getSsh(host);
            LinkedHashMap<String, String> env = new LinkedHashMap<>();
            env.put("JAVA_HOME", host.getJavaHome());
            env.put("PATH", "$JAVA_HOME/bin:$PATH");
//            stderror:, stdout:SERVICE-CENTER(8800) (25550)
//            GATEWAY(8080) (1326)
//            GATEWAY(9000) (2552)
//            AUTH-SERVER(8810) (28691)
//            Total:4
//                    , exitCode=0, expectExitCode:[0]
            SshExecRes serviceStatusRes = null;
            try {
                serviceStatusRes = ssh.sudoSuExecRes(
                        upgradeInfoMgr.getInstallConfig().getInstallUser(), startStopScript + " list -m local",
                        env, 0, 1);
            } catch (Exception e) {
                throw new UpgradeException("failed to get " + type + " status on " + host.getHostName() + ", causeby=" + e.getMessage(), e);
            }

            String serviceName = type.getType().toUpperCase();
            for (String serviceAndPort : serviceStatusRes.getStdOut().split("\n")) {
                if (serviceAndPort.contains(serviceName)) {
                    int portStart = serviceAndPort.indexOf("(");
                    int portEnd = serviceAndPort.indexOf(")");
                    NodeStatus nodeStatus = new NodeStatus(serviceAndPort.substring(portStart + 1, portEnd));
                    if (!serviceAndPort.contains("(-)")) {
                        nodeStatus.setStart(true);
                    }
                    nodeStatusList.add(nodeStatus);
                }
            }
        } finally {
            CommonUtils.closeResource(ssh);
        }
        return nodeStatusList;
    }

    protected String getInstallPath() {
        return upgradeInfoMgr.getInstallConfig().getInstallPath() + "/"
                + type.getInstllPack().getUntarDirName();
    }

    protected void execScript(Ssh ssh, String command, List<NodeStatus> nodeStatusList) throws Exception {
        HostInfo hostInfo = upgradeInfoMgr.getHostInfoWithCheck(ssh.getHost());
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("JAVA_HOME", hostInfo.getJavaHome());
        env.put("PATH", "$JAVA_HOME/bin:$PATH");
        SshExecRes sshExecRes = ssh.sudoSuExecRes(upgradeInfoMgr.getInstallConfig().getInstallUser(), command, env, 0, 1, 2);

        if (sshExecRes.getExitCode() == 1) {
            throw new UpgradeException("failed to stop node, please check remote log:(" + upgradeInfoMgr.getInstallConfig().getInstallPath()
                    + "/log, host=" + ssh.getHost() + ", service=" + getType() + ")");
        }

        if (sshExecRes.getExitCode() == 2) {
            throw new UpgradeException("failed to exec upgrade script, causeby:" + sshExecRes.getStdErr());
        }

        if (!nodeStatusList.isEmpty()) {
            try {
                startNode(ssh, nodeStatusList, env);
            } catch (Exception e) {
                throw new UpgradeException("failed to start node, please check remote log:(" + upgradeInfoMgr.getInstallConfig().getInstallPath()
                        + "/log, host=" + ssh.getHost() + ", service=" + getType() + ")", e);
            }
        }
    }

    protected void startNode(Ssh ssh, List<NodeStatus> nodeStatusList, Map<String, String> env)
            throws Exception {
        for (NodeStatus nodeStatus : nodeStatusList) {
            if (!nodeStatus.isStart()) {
                continue;
            }
            logger.info("Restarting " + type + " node,port=" + nodeStatus.getPort());
            String startCmd = getInstallPath() + "/bin/" + type.getStartStopScript()
                    + " start --timeout " + CommonUtils.getWaitServiceReadyTimeout() + " -p " + nodeStatus.getPort();
            ssh.sudoSuExecRes(upgradeInfoMgr.getInstallConfig().getInstallUser(), startCmd, env);
        }
    }
}
