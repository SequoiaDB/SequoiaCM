package com.sequoiacm.deploy.cleaner;

import com.sequoiacm.deploy.module.InstallPackType;

@Cleaner
public class ConfigServiceCleaner extends ServiceCleanerBase {
    public ConfigServiceCleaner() {
        super(InstallPackType.CONFIG_SERVER, "confctl.sh", "stop -t all -f");
    }
}
