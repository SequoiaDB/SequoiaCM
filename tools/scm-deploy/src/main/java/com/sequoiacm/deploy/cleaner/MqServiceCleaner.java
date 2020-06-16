package com.sequoiacm.deploy.cleaner;

import com.sequoiacm.deploy.module.InstallPackType;

@Cleaner
public class MqServiceCleaner extends ServiceCleanerBase {
    public MqServiceCleaner() {
        super(InstallPackType.MQ_SERVER, "mqctl.sh", "stop -t all -f");
    }
}
