package com.sequoiacm.deploy.cleaner;

import com.sequoiacm.deploy.module.InstallPackType;
@Cleaner
public class CloudServiceCleaner extends ServiceCleanerBase {
    public CloudServiceCleaner() {
        super(InstallPackType.CLOUD, "scmcloudctl.sh", "stop -t all -f");
    }

}
