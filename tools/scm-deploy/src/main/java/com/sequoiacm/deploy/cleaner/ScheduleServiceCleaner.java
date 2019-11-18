package com.sequoiacm.deploy.cleaner;

import com.sequoiacm.deploy.module.InstallPackType;

@Cleaner
public class ScheduleServiceCleaner extends ServiceCleanerBase {
    public ScheduleServiceCleaner() {
        super(InstallPackType.SCHEDULE_SERVER, "schctl.sh", "stop -t all -f");
    }
}
