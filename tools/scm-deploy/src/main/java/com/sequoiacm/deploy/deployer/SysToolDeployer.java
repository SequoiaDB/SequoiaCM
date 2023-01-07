package com.sequoiacm.deploy.deployer;

import com.sequoiacm.deploy.module.NodeInfo;
import com.sequoiacm.deploy.module.ServiceType;
import org.bson.BSONObject;

@Deployer
public class SysToolDeployer extends ServiceDeployerBase {
    public SysToolDeployer() {
        super(ServiceType.SCMSYSTOOLS);
    }

    @Override
    public void deploy(NodeInfo node) throws Exception {
        return;
    }

    @Override
    protected String getStartCmd(NodeInfo node, String serviceRemoteInstallPath, String deployJsonFileRemotePath) {
        return null;
    }

    @Override
    protected BSONObject decorateTemplateDeployJson(BSONObject templateBson, NodeInfo node) throws Exception {
        return null;
    }
}
