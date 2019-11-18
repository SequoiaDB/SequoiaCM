package com.sequoiacm.deploy.cleaner;

import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallPackType;

public interface ServiceCleaner {
    InstallPackType getType();

    public void clean(HostInfo host, boolean dryRun);
}
