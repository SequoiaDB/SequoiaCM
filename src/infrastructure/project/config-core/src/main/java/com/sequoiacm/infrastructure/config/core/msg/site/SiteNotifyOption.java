package com.sequoiacm.infrastructure.config.core.msg.site;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

public class SiteNotifyOption implements NotifyOption {
    private String siteName;
    private Integer version;
    private EventType eventType;

    public SiteNotifyOption(String siteName, Integer version, EventType type) {
        this.siteName = siteName;
        this.version = version;
        this.eventType = type;
    }

    public String getSiteName() {
        return siteName;
    }

    @Override
    public DefaultVersion getVersion() {
        if (eventType == EventType.DELTE) {
            return new DefaultVersion(ScmConfigNameDefine.SITE, siteName, -1);
        }
        return new DefaultVersion(ScmConfigNameDefine.SITE, siteName, version);
    }

    @Override
    public String toString() {
        return "siteName=" + siteName + ",eventType=" + eventType + ",version=" + version;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(ScmRestArgDefine.SITE_CONF_SITENAME, siteName);
        obj.put(ScmRestArgDefine.SITE_CONF_SITEVERSION, version);
        return obj;
    }

    @Override
    public EventType getEventType() {
        return eventType;
    }

}
