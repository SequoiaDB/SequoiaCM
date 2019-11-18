package com.sequoiacm.contentserver.bizconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteNotifyOption;

public class SiteConfSubscriber implements ScmConfSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(SiteConfSubscriber.class);
    private long heartbeatInterval;
    private DefaultVersionFilter versionFilter;
    private String myServiceName;

    public SiteConfSubscriber(String myServiceName, long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
        this.myServiceName = myServiceName;
        this.versionFilter = new DefaultVersionFilter(ScmConfigNameDefine.SITE);
    }

    @Override
    public String subscribeConfigName() {
        return ScmConfigNameDefine.SITE;
    }

    @Override
    public void processNotify(NotifyOption notification) throws Exception {
        logger.info("receive notification:" + notification);
        SiteNotifyOption option = (SiteNotifyOption) notification;
        String siteName = option.getSiteName();
        if (notification.getEventType() == EventType.DELTE) {
            ScmContentServer.getInstance().removeSite(siteName);
            return;
        }
        ScmContentServer.getInstance().reloadSite(siteName);
    }

    @Override
    public VersionFilter getVersionFilter() {
        return versionFilter;
    }

    @Override
    public long getHeartbeatIterval() {
        return this.heartbeatInterval;
    }

    @Override
    public NotifyOption versionToNotifyOption(EventType eventType, Version version) {
        return new SiteNotifyOption(version.getBussinessName(), version.getVersion(), eventType);
    }

    @Override
    public String myServiceName() {
        return myServiceName;
    }

}
