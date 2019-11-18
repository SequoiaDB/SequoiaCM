package com.sequoiacm.infrastructure.config.core.msg.site;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

public class SiteFilter implements ConfigFilter {

    private String siteName;

    public SiteFilter(String siteName) {
        this.siteName = siteName;
    }

    public String getSiteName() {
        return siteName;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        if (siteName != null) {
            obj.put(ScmRestArgDefine.SITE_CONF_SITENAME, siteName);
        }
        return obj;

    }
}
