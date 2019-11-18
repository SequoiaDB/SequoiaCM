package com.sequoiacm.deploy.cleaner;

import com.sequoiacm.deploy.module.InstallPackType;

@Cleaner
public class OmServiceCleaner extends ServiceCleanerBase {
    public OmServiceCleaner() {
        super(InstallPackType.OM_SERVER, "omctl.sh", "stop -t all -f");
    }
}
