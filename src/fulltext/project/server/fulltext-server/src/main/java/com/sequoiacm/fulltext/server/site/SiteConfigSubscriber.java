package com.sequoiacm.fulltext.server.site;

import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteNotifyOption;

public class SiteConfigSubscriber implements ScmConfSubscriber {
    private ScmSiteInfoMgr siteInfoMgr;
    private long heartbeatInterval;

    public SiteConfigSubscriber(ScmSiteInfoMgr siteInfoMgr, long hearbeatInterval) {
        this.siteInfoMgr = siteInfoMgr;
        this.heartbeatInterval = hearbeatInterval;
    }

    @Override
    public String myServiceName() {
        return "fulltext-server";
    }

    @Override
    public String subscribeConfigName() {
        return ScmConfigNameDefine.SITE;
    }

    @Override
    public void processNotify(NotifyOption notification) throws Exception {
        SiteNotifyOption siteNotify = (SiteNotifyOption) notification;
        if (siteNotify.getEventType() == EventType.DELTE) {
            siteInfoMgr.removeSite(siteNotify.getSiteName());
            return;
        }

        if (siteNotify.getEventType() == EventType.CREATE) {
            siteInfoMgr.addSite(siteNotify.getSiteName());
            return;
        }
    }

    @Override
    public VersionFilter getVersionFilter() {
        return new DefaultVersionFilter(ScmConfigNameDefine.SITE);
    }

    @Override
    public long getHeartbeatIterval() {
        return heartbeatInterval;
    }

    @Override
    public NotifyOption versionToNotifyOption(EventType eventType, Version version) {
        DefaultVersion defaultVersion = (DefaultVersion) version;
        return new SiteNotifyOption(defaultVersion.getBussinessName(), defaultVersion.getVersion(),
                eventType);
    }
}
