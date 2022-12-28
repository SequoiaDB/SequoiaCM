package com.sequoiacm.deploy.rollbacker;

import com.sequoiacm.deploy.module.ServiceType;
import com.sequoiacm.deploy.module.StatusInfo;

public interface ServiceRollbacker {
    void rollback(StatusInfo upgradeStatus) throws Exception;

    ServiceType getType();
}
