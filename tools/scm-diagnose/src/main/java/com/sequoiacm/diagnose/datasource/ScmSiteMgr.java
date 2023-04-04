package com.sequoiacm.diagnose.datasource;

import java.util.HashMap;
import java.util.Map;

import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.datasource.dataservice.ScmService;

public class ScmSiteMgr {
    private Map<Integer, ScmSite> siteIdMap = new HashMap<>();
    private Map<Integer, ScmSiteInfo> siteIdInfoMap = new HashMap<>();
    private Map<String, ScmSiteInfo> siteNameInfoMap = new HashMap<>();

    public ScmSiteMgr() {
    }

    public void addScmSite(int siteId, ScmSite site) {
        if (null == site) {
            return;
        }
        siteIdMap.put(siteId, site);
    }

    public ScmSiteInfo getSite(int siteId) {
        return siteIdInfoMap.get(siteId);
    }

    public ScmSiteInfo getSite(String siteName) {
        return siteNameInfoMap.get(siteName);
    }

    public void addScmSiteInfo(ScmSiteInfo info) {
        siteIdInfoMap.put(info.getId(), info);
        siteNameInfoMap.put(info.getName(), info);
    }

    public ScmService getScmDataScmService(int siteId) {
        ScmSite scmSite = siteIdMap.get(siteId);
        if (scmSite == null) {
            return null;
        }
        return scmSite.getDataService();
    }

    public ScmDataOpFactory getScmDataOpFactory(int siteId) {
        ScmSite scmSite = siteIdMap.get(siteId);
        if (scmSite == null) {
            return null;
        }
        return scmSite.getOpFactory();
    }
}
