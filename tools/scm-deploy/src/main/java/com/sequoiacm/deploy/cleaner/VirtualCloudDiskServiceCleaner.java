package com.sequoiacm.deploy.cleaner;

import com.sequoiacm.deploy.module.InstallPackType;

@Cleaner
public class VirtualCloudDiskServiceCleaner extends ServiceCleanerBase {
    public VirtualCloudDiskServiceCleaner() {
        super(InstallPackType.VIRTUAL_CLOUD_DISK, "stop.sh", "");
    }
}
