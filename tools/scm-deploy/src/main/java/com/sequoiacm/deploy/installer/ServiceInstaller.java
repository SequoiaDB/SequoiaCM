package com.sequoiacm.deploy.installer;

import com.sequoiacm.deploy.module.HostInfo;
import com.sequoiacm.deploy.module.InstallPackType;

public interface ServiceInstaller {
    public String install(HostInfo host) throws Exception;

    public InstallPackType getType();
}
