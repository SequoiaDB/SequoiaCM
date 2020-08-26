package com.sequoiacm.schedule.core.meta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.schedule.bizconf.ScheduleStrategyMgr;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.core.ScheduleServer;

public class SiteMgr {
    private static final Logger logger = LoggerFactory.getLogger(SiteMgr.class);
    private Map<Integer, SiteInfo> siteMap = new HashMap<>();
    private Map<String, SiteInfo> siteMapByName = new HashMap<>();
    private ReentrantReadWriteLock siteReadWriterLock = new ReentrantReadWriteLock();
    private SiteInfo rootSite;

    public void initSiteMap(Map<Integer, SiteInfo> siteMap, Map<String, SiteInfo> siteMapByName) {
        this.siteMap = siteMap;
        this.siteMapByName = siteMapByName;
    }

    public void initRootSite(SiteInfo rootSite) {
        this.rootSite = rootSite;
    }

    public void reloadSite(SiteInfo siteInfo) throws Exception {
        WriteLock wLock = siteReadWriterLock.writeLock();
        wLock.lock();
        try {
            if (siteInfo.isRoot()) {
                // create root site, initialize strategy management (star model)
                // user create strategy record by myself
                List<BSONObject> strategyList = ScheduleServer.getInstance().getAllStrategy();
                ScheduleStrategyMgr.getInstance().init(strategyList, siteInfo);
                rootSite = siteInfo;

            }
            siteMapByName.put(siteInfo.getName(), siteInfo);
            siteMap.put(siteInfo.getId(), siteInfo);

            logger.info("reload site cache:siteName={}", siteInfo.getName());
        }
        finally {
            wLock.unlock();
        }
    }

    public void removeSite(String siteName) throws ScheduleException {
        WriteLock wLock = siteReadWriterLock.writeLock();
        wLock.lock();
        try {
            if (rootSite != null && rootSite.getName().equals(siteName)) {
                // clear root site strategy cache
                ScheduleStrategyMgr.getInstance().removeRootSite(rootSite.getId());
                rootSite = null;
            }

            SiteInfo siteInfo = siteMapByName.remove(siteName);
            if (siteInfo != null) {
                siteMap.remove(siteInfo.getId());
                ScheduleServer.getInstance().removeNodesBySiteId(siteInfo.getId());
                logger.info("remove site cache:siteName={}", siteName);
            }
        }
        finally {
            wLock.unlock();
        }
    }

    public SiteInfo getSite(int siteId) {
        Lock rLock = siteReadWriterLock.readLock();
        rLock.lock();
        try {
            return siteMap.get(siteId);
        }
        finally {
            rLock.unlock();
        }
    }

    public SiteInfo getMainSite() {
        return this.rootSite;
    }
}
