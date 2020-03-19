package com.sequoiacm.deploy.cleaner;

import com.sequoiacm.deploy.module.InstallPackType;

@Cleaner
public class S3ServiceCleaner extends ServiceCleanerBase {
    public S3ServiceCleaner() {
        super(InstallPackType.S3_SERVER, "s3ctl.sh", "stop -t all -f");
    }
}
