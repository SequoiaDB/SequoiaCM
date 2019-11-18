package com.sequoiacm.deploy.cleaner;

import com.sequoiacm.deploy.module.InstallPackType;

@Cleaner
public class ContentserverCleaner extends ServiceCleanerBase {
    public ContentserverCleaner() {
        super(InstallPackType.CONTENTSERVER, "scmctl.sh", "stop -a  -f");
    }
}
