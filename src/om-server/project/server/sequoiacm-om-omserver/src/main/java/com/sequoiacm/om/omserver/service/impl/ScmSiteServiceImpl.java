package com.sequoiacm.om.omserver.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequoiacm.om.omserver.config.ScmOmServerConfig;
import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.dao.ScmSiteDao;
import com.sequoiacm.om.omserver.factory.ScmSiteDaoFactory;
import com.sequoiacm.om.omserver.module.OmCacheWrapper;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmSiteInfo;
import com.sequoiacm.om.omserver.service.ScmSiteService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@Service
public class ScmSiteServiceImpl implements ScmSiteService {

    private OmCacheWrapper<String> siteStrategyCache = null;
    private long siteCacheTTL;

    @Autowired
    private ScmSiteChooser siteChooser;

    @Autowired
    private ScmSiteDaoFactory scmSiteDaoFactory;

    @Autowired
    public ScmSiteServiceImpl(ScmOmServerConfig config) {
        this.siteCacheTTL = config.getCacheRefreshInterval() * 1000L;
    }

    @Override
    public String getSiteById(ScmOmSession session, int id)
            throws ScmInternalException, ScmOmServerException {
        List<OmSiteInfo> sitesInfo = scmSiteDaoFactory.createSiteDao(session)
                .listSite(new BasicBSONObject("id", id), 0, 1);
        if (sitesInfo.isEmpty()) {
            throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR,
                    "site not exist:siteId=" + id);
        }
        return sitesInfo.get(0).getName();
    }
    @Override
    public List<OmSiteInfo> getSiteList(ScmOmSession session, BSONObject filter, long skip,
            long limit) throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        try {
            session.resetServiceEndpoint(preferSite);
            return scmSiteDaoFactory.createSiteDao(session).listSite(filter, skip, limit);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    public long getSiteCount(ScmOmSession session, BSONObject filter)
            throws ScmOmServerException, ScmInternalException {
        String preferSite = siteChooser.chooseFromAllSite();
        ScmSiteDao siteDao = scmSiteDaoFactory.createSiteDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            try {
                return siteDao.getSiteCount(filter);
            }
            catch (Exception e) {
                return siteDao.listSite(null, 0, -1).size();
            }
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
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

    @Override
    public Map<String, OmSiteInfo> getSitesAsMap(ScmOmSession session)
            throws ScmOmServerException, ScmInternalException {
        ScmSiteDao siteDao = scmSiteDaoFactory.createSiteDao(session);
        List<OmSiteInfo> sites = siteDao.listSite(null, 0, -1);
        Map<String, OmSiteInfo> map = new HashMap<>();
        for (OmSiteInfo site : sites) {
            map.put(site.getName(), site);
        }
        return map;
    }

}
