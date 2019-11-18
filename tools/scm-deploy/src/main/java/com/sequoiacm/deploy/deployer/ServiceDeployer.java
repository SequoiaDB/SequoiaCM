package com.sequoiacm.deploy.deployer;

import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;

public interface ServiceDeployer {

    public ServiceType getServiceType();

    void deploy(NodeInfo node) throws Exception;
}
