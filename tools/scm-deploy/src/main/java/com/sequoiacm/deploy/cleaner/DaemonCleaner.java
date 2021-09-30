package com.sequoiacm.deploy.cleaner;

import com.sequoiacm.deploy.module.InstallPackType;

@Cleaner
public class DaemonCleaner extends ServiceCleanerBase {
    public DaemonCleaner() {
        super(InstallPackType.DAEMON, "scmd.sh", "stop");
    }
}
