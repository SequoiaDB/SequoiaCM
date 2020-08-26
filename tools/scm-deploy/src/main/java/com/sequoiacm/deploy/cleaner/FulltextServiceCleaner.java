package com.sequoiacm.deploy.cleaner;

import com.sequoiacm.deploy.module.InstallPackType;

@Cleaner
public class FulltextServiceCleaner extends ServiceCleanerBase {
    public FulltextServiceCleaner() {
        super(InstallPackType.FULLTEXT_SERVER, "ftctl.sh", "stop -t all -f");
    }
}
