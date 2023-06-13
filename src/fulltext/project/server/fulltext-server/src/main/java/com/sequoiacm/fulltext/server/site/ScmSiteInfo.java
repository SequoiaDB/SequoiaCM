package com.sequoiacm.fulltext.server.site;

import com.sequoiacm.infrastructure.config.core.msg.site.SiteConfig;

public class ScmSiteInfo {
    private boolean isRoot;
    private String name;
    private int siteId;
    public ScmSiteInfo(SiteConfig cfg) {
        this.isRoot = cfg.isRootSite();
        this.name = cfg.getName();
        this.siteId = cfg.getId();
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

}
