package com.sequoiacm.deploy.upgrader;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.NodeStatus;
import com.sequoiacm.deploy.module.ServiceType;

@Upgrader
public class SysToolsUpgrader extends ServiceUpgraderBase {

    public SysToolsUpgrader() {
        super(ServiceType.SCMSYSTOOLS);
    }

    @Override
    public List<NodeStatus> getNodeStatus(HostInfo host) {
        return new ArrayList<>();
    }
}
