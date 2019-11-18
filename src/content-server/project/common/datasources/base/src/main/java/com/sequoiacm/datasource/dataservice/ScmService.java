package com.sequoiacm.datasource.dataservice;

import com.sequoiacm.datasource.metadata.ScmSiteUrl;

public abstract class ScmService {
    protected int siteId;
    protected ScmSiteUrl siteUrl;

    public ScmService(int siteId, ScmSiteUrl siteUrl) {
        this.siteId = siteId;
        this.siteUrl = siteUrl;
    }

    public int getSiteId() {
        return siteId;
    }

    public ScmSiteUrl getSiteUrl() {
        return siteUrl;
    }

    public boolean supportsBreakpointUpload() {
        return false;
    }

    public abstract String getType();
    public abstract void clear();


}
