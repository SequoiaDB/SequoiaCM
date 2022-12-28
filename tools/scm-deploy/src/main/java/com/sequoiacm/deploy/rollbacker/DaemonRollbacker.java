package com.sequoiacm.deploy.rollbacker;

import com.sequoiacm.deploy.module.NodeStatus;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.ssh.Ssh;
import com.sequoiacm.deploy.upgrader.DaemonUpgrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Rollbacker
public class DaemonRollbacker extends ServiceRollbackerBase {
    private final static Logger logger = LoggerFactory.getLogger(DaemonUpgrader.class);

    public DaemonRollbacker() {
        super(ServiceType.DAEMON);
    }

    @Override
    protected void startNode(Ssh ssh, List<NodeStatus> nodeStatusList, Map<String, String> env)
            throws Exception {
        logger.info("Restarting " + type + " node");
        String startCmd = getInstallPath() + "/bin/" + type.getStartStopScript() + " start";
        ssh.sudoSuExecRes(upgradeStatusInfoMgr.getInstallConfig().getInstallUser(), startCmd, env);
    }
}
