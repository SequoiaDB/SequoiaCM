package com.sequoiacm.deploy.cleaner;

import com.sequoiacm.deploy.module.InstallPackType;

@Cleaner
public class ScmSysToolsServiceCleaner extends ServiceCleanerBase {

    public ScmSysToolsServiceCleaner(){
        super(InstallPackType.SCMSYSTOOLS, "nothing", "");
    }
}
