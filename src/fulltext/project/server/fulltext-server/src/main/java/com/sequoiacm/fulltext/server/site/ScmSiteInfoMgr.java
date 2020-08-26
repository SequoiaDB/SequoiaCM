package com.sequoiacm.fulltext.server.site;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.ConfServiceClient;
import com.sequoiacm.fulltext.server.config.ConfVersionConfig;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteConfig;

@Component
public class ScmSiteInfoMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmSiteInfoMgr.class);

    private ConfServiceClient confClient;
    private ReentrantReadWriteLock sitesLock = new ReentrantReadWriteLock();
    private List<ScmSiteInfo> sites = new ArrayList<>();

    @Autowired
    public ScmSiteInfoMgr(ConfServiceClient confClient, DiscoveryClient dicoveryClient,
            ConfVersionConfig versionConfig) throws FullTextException {
        this.confClient = confClient;

        confClient.registerSubscriber(
                new SiteConfigSubscriber(this, versionConfig.getSiteHeartbeat()));

        List<SiteConfig> siteList = confClient.getSiteList();
        WriteLock wirteLock = sitesLock.writeLock();
        wirteLock.lock();
        try {
            for (SiteConfig site : siteList) {
                ScmSiteInfo e = new ScmSiteInfo();
                e.setName(site.getName());
                e.setSiteId(site.getId());
                e.setRoot(site.isRootSite());
                sites.add(e);
            }
        }
        finally {
            wirteLock.unlock();
        }

    }

    public ScmSiteInfo getRootSite() throws FullTextException {
        ReadLock readLock = sitesLock.readLock();
        readLock.lock();
        try {
            for (ScmSiteInfo site : sites) {
                if (site.isRoot()) {
                    return site;
                }
            }
        }
        finally {
            readLock.unlock();
        }
        throw new FullTextException(ScmError.SYSTEM_ERROR, "root site not exist");
    }

    //
    //    public boolean isSameZoneWithMe(int siteId) {
    //        String myRegionZone = region + "-" + zone;
    //        ReadLock readLock = sitesLock.readLock();
    //        readLock.lock();
    //        try {
    //            for (ScmSiteInfo site : sites) {
    //                if (siteId != site.getSiteId()) {
    //                    continue;
    //                }
    //                for (String regionZone : site.getZones()) {
    //                    if (regionZone.equals(myRegionZone)) {
    //                        return true;
    //                    }
    //                }
    //                return false;
    //            }
    //            return false;
    //        }
    //        finally {
    //            readLock.unlock();
    //        }
    //    }
    //
    //    public String getPreferSite(List<Integer> expectedSites) throws FullTextException {
    //        String myRegionZone = region + "-" + zone;
    //        String sameRegionSite = null;
    //        List<String> expectedSiteNames = new ArrayList<>();
    //        ReadLock readLock = sitesLock.readLock();
    //        readLock.lock();
    //        try {
    //            for (ScmSiteInfo site : sites) {
    //                if (!expectedSites.contains(site.getSiteId())) {
    //                    expectedSiteNames.add(site.getName());
    //                    continue;
    //                }
    //                for (String regionZone : site.getZones()) {
    //                    if (regionZone.equals(myRegionZone)) {
    //                        return site.getName();
    //                    }
    //                    if (sameRegionSite != null) {
    //                        continue;
    //                    }
    //                    if (regionZone.startsWith(region + "-")) {
    //                        sameRegionSite = site.getName();
    //                    }
    //                }
    //            }
    //        }
    //        finally {
    //            readLock.unlock();
    //        }
    //
    //        if (sameRegionSite != null) {
    //            return sameRegionSite;
    //        }
    //
    //        if (expectedSiteNames.size() <= 0) {
    //            throw new FullTextException(ScmError.SITE_NOT_EXIST,
    //                    "no such site:siteIdList=" + expectedSites);
    //        }
    //
    //        Random random = new Random();
    //        int rIdx = random.nextInt(expectedSiteNames.size());
    //        return expectedSiteNames.get(rIdx);
    //    }

    void removeSite(String name) {
        WriteLock wirteLock = sitesLock.writeLock();
        wirteLock.lock();
        try {
            Iterator<ScmSiteInfo> it = sites.iterator();
            while (it.hasNext()) {
                ScmSiteInfo site = it.next();
                if (site.getName().equals(name)) {
                    it.remove();
                }
            }
        }
        finally {
            wirteLock.unlock();
        }
    }

    void addSite(String siteName) throws FullTextException {
        SiteConfig site = confClient.getSite(siteName);
        if (site == null) {
            return;
        }
        ScmSiteInfo info = new ScmSiteInfo();
        info.setName(site.getName());
        info.setSiteId(site.getId());
        WriteLock wirteLock = sitesLock.writeLock();
        wirteLock.lock();
        try {
            sites.add(info);
        }
        finally {
            wirteLock.unlock();
        }
    }

    public String getSiteNameById(int id) throws FullTextException {
        ReadLock readLock = sitesLock.readLock();
        readLock.lock();
        try {
            for (ScmSiteInfo site : sites) {
                if (site.getSiteId() == id) {
                    return site.getName();
                }
            }
        }
        finally {
            readLock.unlock();
        }

        throw new FullTextException(ScmError.SITE_NOT_EXIST, "no such site:id=" + id);
    }

    public String getRootSiteName() throws FullTextException {
        return getRootSite().getName();
    }

    public int getRootSiteId() throws FullTextException {
        return getRootSite().getSiteId();
    }

}
