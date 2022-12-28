package com.sequoiacm.deploy.upgrader;

import com.sequoiacm.deploy.common.CommonUtils;
import com.sequoiacm.deploy.exception.UpgradeException;
import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.NodeStatus;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.ssh.SshExecRes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Upgrader
public class DaemonUpgrader extends ServiceUpgraderBase {
    private final static Logger logger = LoggerFactory.getLogger(DaemonUpgrader.class);
    private String cronPropPath;
    private final String CONF = "conf";
    private final String CRON_PROPERTIES = ".crontab.properties";
    private final String CRON_LINUX = "linuxCron";
    private final String NOHUP = "nohup";

    public DaemonUpgrader() {
        super(ServiceType.DAEMON);
        this.cronPropPath = upgradeInfoMgr.getInstallConfig().getInstallPath() + File.separator
                + ServiceType.DAEMON.getInstllPack().getUntarDirName() + File.separator + CONF
                + File.separator + CRON_PROPERTIES;
    }

    @Override
    public List<NodeStatus> getNodeStatus(HostInfo host) throws Exception {
        Ssh ssh = null;
        try {
            ssh = sshFactory.getSsh(host);
            SshExecRes cronConfRes = ssh.exec("ls " + cronPropPath, 0, 2);
            if (cronConfRes.getExitCode() != 0) {
                return new ArrayList<>();
            }
            String sedCronCommand = "sed -n '/" + CRON_LINUX + "/p' " + cronPropPath;
            SshExecRes sedCronRes = ssh.exec(sedCronCommand, 0, 2);
            if (sedCronRes.getExitCode() != 0 || sedCronRes.getStdOut().length() <= 0) {
                return  new ArrayList<>();
            }

            String processMatcher = sedCronRes.getStdOut()
                    .substring(sedCronRes.getStdOut().indexOf(NOHUP) + NOHUP.length(),
                            sedCronRes.getStdOut().indexOf(">"))
                    .trim();
            String processCommand = "ps -eo pid,cmd | grep -w \"" + processMatcher
                    + "\"| grep -w -v grep";
            SshExecRes processRes = null;
            try {
                processRes = ssh.exec(processCommand, 0, 1);
            } catch (Exception e) {
                throw new UpgradeException("failed to get " + type + " status on " + host.getHostName() + ", causeby=" + e.getMessage(), e);
            }
            if (processRes.getExitCode() == 0) {
                return Collections.singletonList(new NodeStatus("0", true));
            }
            return new ArrayList<>();
        } finally {
            CommonUtils.closeResource(ssh);
        }
    }

    @Override
    protected void startNode(Ssh ssh, List<NodeStatus> nodeStatusList, Map<String, String> env)
            throws Exception {
        logger.info("Restarting " + type + " node");
        String startCmd = getInstallPath() + "/bin/" + type.getStartStopScript() + " start";
        ssh.sudoSuExecRes(upgradeInfoMgr.getInstallConfig().getInstallUser(), startCmd, env);
    }
}