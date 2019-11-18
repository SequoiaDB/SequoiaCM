package com.sequoiacm.cloud.adminserver.model;

import java.util.HashMap;
import java.util.Map;

public class SiteMgr {
    private Map<Integer, SiteInfo> siteMap = new HashMap<>();
    private SiteInfo rootSite;

    public void addSite(SiteInfo site) {
        if (site.isRoot()) {
            this.rootSite = site;
        }
        siteMap.put(site.getId(), site);
    }

    public SiteInfo getSite(int siteId) {
        return siteMap.get(siteId);
    }
    
    public SiteInfo getMainSite() {
        return this.rootSite;
    }
}
