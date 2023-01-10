package com.sequoiacm.infrastructure.config.core.msg.site;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class SiteUpdator implements ConfigUpdator {
    private String siteName;
    private String updateStageTag;

    public SiteUpdator(String siteName, String updateStageTag) {
        this.siteName = siteName;
        this.updateStageTag = updateStageTag;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getUpdateStageTag() {
        return updateStageTag;
    }

    public void setUpdateStageTag(String updateStageTag) {
        this.updateStageTag = updateStageTag;
    }

    @Override
    public BSONObject toBSONObject() {
        BSONObject obj = new BasicBSONObject();
        obj.put(ScmRestArgDefine.SITE_CONF_SITENAME, siteName);
        BSONObject updator = new BasicBSONObject();
        updator.put(ScmRestArgDefine.SITE_CONF_STAGETAG, updateStageTag);
        obj.put(ScmRestArgDefine.SITE_CONF_UPDATOR, updator);
        return obj;
    }
}
