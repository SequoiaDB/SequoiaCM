package com.sequoiacm.om.omserver.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequoiacm.om.omserver.factory.ScmSiteDaoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.om.omserver.config.ScmOmServerConfig;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmSiteInfo;
import com.sequoiacm.om.omserver.module.OmCacheWrapper;
import com.sequoiacm.om.omserver.service.ScmSiteService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@Service
public class ScmSiteServiceImpl implements ScmSiteService {

    private Map<Integer, OmCacheWrapper<OmSiteInfo>> sitesCache = new HashMap<>();
    private OmCacheWrapper<String> siteStrategyCache = null;
    private long siteCacheTTL;

    @Autowired
    private ScmSiteDaoFactory scmSiteDaoFactory;

    @Autowired
    public ScmSiteServiceImpl(ScmOmServerConfig config) {
        this.siteCacheTTL = config.getCacheRefreshInterval() * 1000;
    }

    @Override
    public String getSiteById(ScmOmSession session, int id)
            throws ScmInternalException, ScmOmServerException {
        OmCacheWrapper<OmSiteInfo> cache = sitesCache.get(id);
        if (cache != null && !cache.isExpire(siteCacheTTL)) {
            return cache.getCache().getName();
        }

        OmSiteInfo site = refreshCacheAndGet(session, id);
        if (site == null) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "site not exist:siteId=" + id);
        }
        return site.getName();
    }

    @Override
    public List<OmSiteInfo> getSiteList(ScmOmSession session)
            throws ScmOmServerException, ScmInternalException {
        for (Map.Entry<Integer, OmCacheWrapper<OmSiteInfo>> entry : sitesCache.entrySet()) {
            if (!entry.getValue().isExpire(siteCacheTTL)) {
                return getSiteListFromCacheMap(sitesCache);
            }
            break;
        }
        refreshCacheAndGet(session, -1);
        return getSiteListFromCacheMap(sitesCache);
    }

    private List<OmSiteInfo> getSiteListFromCacheMap(
            Map<Integer, OmCacheWrapper<OmSiteInfo>> sitesCache) {
        List<OmSiteInfo> omSiteInfoList = new ArrayList<>();
        if (sitesCache == null || sitesCache.size() <= 0) {
            return omSiteInfoList;
        }
        for (OmCacheWrapper<OmSiteInfo> cache : sitesCache.values()) {
            omSiteInfoList.add(cache.getCache());
        }
        return omSiteInfoList;
    }

    @Override
    public String getSiteStrategy(ScmOmSession session) throws ScmInternalException {
        if (siteStrategyCache != null && !siteStrategyCache.isExpire(siteCacheTTL)) {
            return siteStrategyCache.getCache();
        }
        String siteStrategy = scmSiteDaoFactory.createSiteDao(session).getSiteStrategy();
        siteStrategyCache = new OmCacheWrapper<>(siteStrategy);
        return siteStrategy;
    }

    private OmSiteInfo refreshCacheAndGet(ScmOmSession session, int siteId)
            throws ScmOmServerException, ScmInternalException {
        Map<Integer, OmCacheWrapper<OmSiteInfo>> sitesCache = new HashMap<>();
        List<OmSiteInfo> sitesInfo = scmSiteDaoFactory.createSiteDao(session).listSite();
        OmSiteInfo ret = null;
        for (OmSiteInfo siteInfo : sitesInfo) {
            if (siteId == siteInfo.getId()) {
                ret = siteInfo;
            }
            sitesCache.put(siteInfo.getId(), new OmCacheWrapper<OmSiteInfo>(siteInfo));
        }
        this.sitesCache = sitesCache;
        return ret;
    }

}
