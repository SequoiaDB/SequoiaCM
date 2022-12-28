package com.sequoiacm.deploy.upgrader;

import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.NodeStatus;
import com.sequoiacm.deploy.module.ServiceType;

import java.util.ArrayList;
import java.util.List;

@Upgrader
public class NonServiceUpgrader extends ServiceUpgraderBase {

    public NonServiceUpgrader() {
        super(ServiceType.NON_SERVICE);
    }

    @Override
    public List<NodeStatus> getNodeStatus(HostInfo host) {
        return new ArrayList<>();
    }

    @Override
    protected String getInstallPath() {
        return upgradeInfoMgr.getInstallConfig().getInstallPath();
    }
}
