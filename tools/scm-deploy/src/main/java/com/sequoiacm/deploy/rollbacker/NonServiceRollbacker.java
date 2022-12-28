package com.sequoiacm.deploy.rollbacker;

import com.sequoiacm.deploy.module.ServiceType;

@Rollbacker
public class NonServiceRollbacker extends ServiceRollbackerBase {

    public NonServiceRollbacker() {
        super(ServiceType.NON_SERVICE);
    }

    @Override
    public String getInstallPath() {
        return upgradeStatusInfoMgr.getInstallConfig().getInstallPath();
    }
}
