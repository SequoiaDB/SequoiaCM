package com.sequoiacm.deploy.upgrader;

import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.NodeStatus;
import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.module.StatusInfo;

import java.util.List;

public interface ServiceUpgrader {
    ServiceType getType();

    void upgrade(StatusInfo statusInfo) throws Exception;

    List<NodeStatus> getNodeStatus(HostInfo host) throws Exception;
}
