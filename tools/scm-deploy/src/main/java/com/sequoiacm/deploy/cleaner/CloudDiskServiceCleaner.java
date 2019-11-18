package com.sequoiacm.deploy.cleaner;

import com.sequoiacm.deploy.module.InstallPackType;

@Cleaner
public class CloudDiskServiceCleaner extends ServiceCleanerBase {
    public CloudDiskServiceCleaner() {
        super(InstallPackType.CLOUD_DISK, "cdiskctl.sh", "stop -t all -f");
    }
}
