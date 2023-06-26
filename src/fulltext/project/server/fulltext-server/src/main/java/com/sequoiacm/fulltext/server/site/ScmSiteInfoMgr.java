package com.sequoiacm.fulltext.server.site;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.ConfServiceClient;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteConfig;

import javax.annotation.PreDestroy;

@Component
public class ScmSiteInfoMgr {

    private static final Logger logger = LoggerFactory.getLogger(ScmSiteInfoMgr.class);
    private final ConfServiceClient confClient;
    private final Map<String, ScmSiteInfo> sites = new ConcurrentHashMap<>();

    private ScmTimer initSiteTimer = null;

    private volatile boolean isInitialized = false;

    @Autowired
    public ScmSiteInfoMgr(ConfServiceClient confClient)
            throws FullTextException, ScmConfigException {
        this.confClient = confClient;
        try {
            init();
        }
        catch (Exception e) {
            logger.warn("failed to initialize site info manager, retry later: ", e);
            asyncReInit(this, 5000);
        }
    }

    private void init() throws ScmConfigException, FullTextException {
        List<SiteConfig> siteList = confClient.getSiteList();
        for (SiteConfig site : siteList) {
            ScmSiteInfo e = new ScmSiteInfo(site);
            sites.put(e.getName(), e);
        }
        confClient.subscribe(ScmBusinessTypeDefine.SITE, this::onSiteNotify);
        isInitialized = true;
    }

    private void onSiteNotify(EventType type, String businessName, NotifyOption notification)
            throws FullTextException {
        if (type == EventType.DELTE) {
            removeSite(businessName);
            return;
        }

        refresh(businessName);
    }

    public ScmSiteInfo getRootSite() throws FullTextException {
        for (ScmSiteInfo site : sites.values()) {
            if (site.isRoot()) {
                return site;
            }
        }
        throw new FullTextException(ScmError.SYSTEM_ERROR, "root site not exist");
    }


    void removeSite(String name) {
        sites.remove(name);
    }

    void refresh(String siteName) throws FullTextException {
        SiteConfig site = confClient.getSite(siteName);
        if (site == null) {
            sites.remove(siteName);
            return;
        }
        ScmSiteInfo info = new ScmSiteInfo(site);
        sites.put(siteName, info);
    }

    public String getSiteNameById(int id) throws FullTextException {
        checkIsInited();
        for (ScmSiteInfo site : sites.values()) {
            if (site.getSiteId() == id) {
                return site.getName();
            }
        }
        throw new FullTextException(ScmError.SITE_NOT_EXIST, "no such site:id=" + id);
    }

    public String getRootSiteName() throws FullTextException {
        checkIsInited();
        return getRootSite().getName();
    }

    public int getRootSiteId() throws FullTextException {
        checkIsInited();
        return getRootSite().getSiteId();
    }

    private void checkIsInited() {
        if (!isInitialized) {
            throw new IllegalStateException("Site info manager is not init yet");
        }
    }

    private void asyncReInit(ScmSiteInfoMgr siteInfoMgr, int interval) {
        if (initSiteTimer == null) {
            initSiteTimer = ScmTimerFactory.createScmTimer();
        }
        initSiteTimer.schedule(new ScmTimerTask() {
            @Override
            public void run() {
                try {
                    siteInfoMgr.init();
                    cancel();
                }
                catch (Exception e) {
                    logger.warn("failed to initialize site info manager", e);
                }
            }
        }, interval, interval);
    }

    @PreDestroy
    private void destroy() {
        if (initSiteTimer != null) {
            initSiteTimer.cancel();
        }
    }
}