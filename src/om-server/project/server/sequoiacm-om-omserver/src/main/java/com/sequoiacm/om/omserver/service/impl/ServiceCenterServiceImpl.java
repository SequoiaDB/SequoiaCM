package com.sequoiacm.om.omserver.service.impl;

import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.om.omserver.config.ScmOmServerConfig;
import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.dao.ScmServiceCenterDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.factory.ScmServiceCenterDaoFactory;
import com.sequoiacm.om.omserver.module.OmCacheWrapper;
import com.sequoiacm.om.omserver.service.ServiceCenterService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ServiceCenterServiceImpl implements ServiceCenterService {
    @Autowired
    private ScmSiteChooser siteChooser;
    @Autowired
    private ScmServiceCenterDaoFactory scmServiceCenterDaoFactory;

    private long cacheTTL;

    private OmCacheWrapper<List<ScmServiceInstance>> serviceListCache = null;

    @Autowired
    public ServiceCenterServiceImpl(ScmOmServerConfig config) {
        this.cacheTTL = config.getCacheRefreshInterval() * 1000L;
    }

    @Override
    public Set<String> listZones(ScmOmSession session, String region) throws ScmInternalException {
        List<ScmServiceInstance> serviceList = getServiceList(session);
        Set<String> zones = new HashSet<>();
        if (serviceList == null || serviceList.size() <= 0) {
            return zones;
        }
        for (ScmServiceInstance scmServiceInstance : serviceList) {
            if (scmServiceInstance.getRegion().equals(region)) {
                zones.add(scmServiceInstance.getZone());
            }
        }
        return zones;
    }

    @Override
    public Set<String> listRegions(ScmOmSession session)
            throws ScmOmServerException, ScmInternalException {
        List<ScmServiceInstance> serviceList = getServiceList(session);
        Set<String> regions = new HashSet<>();
        if (serviceList == null || serviceList.size() <= 0) {
            return regions;
        }
        for (ScmServiceInstance scmServiceInstance : serviceList) {
            regions.add(scmServiceInstance.getRegion());
        }
        return regions;
    }

    @Override
    public List<ScmServiceInstance> getServiceList(ScmOmSession session)
            throws ScmInternalException {
        if (serviceListCache != null && !serviceListCache.isExpire(cacheTTL)) {
            return serviceListCache.getCache();
        }
        try {
            ScmServiceCenterDao serviceCenterDao = scmServiceCenterDaoFactory
                    .createServiceCenterDao(session);
            List<ScmServiceInstance> serviceList = serviceCenterDao.getServiceList();
            serviceListCache = new OmCacheWrapper<>(serviceList);
            return serviceList;
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

}
