package com.sequoiacm.infrastructure.config.core.msg.site;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdater;

@BusinessType(ScmBusinessTypeDefine.SITE)
public class SiteUpdater implements ConfigUpdater {

    @JsonProperty(ScmRestArgDefine.SITE_CONF_SITENAME)
    private String siteName;

    @JsonProperty(ScmRestArgDefine.SITE_CONF_UPDATOR)
    private Updater updater = new Updater();
    public SiteUpdater(String siteName, String updateStageTag) {
        this.siteName = siteName;
        updater.setUpdateStageTag(updateStageTag);
    }

    public SiteUpdater() {
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getUpdateStageTag() {
        return updater.getUpdateStageTag();
    }

    public Updater getUpdater() {
        return updater;
    }

    public void setUpdater(Updater updater) {
        this.updater = updater;
    }
}

class Updater{

    @JsonProperty(ScmRestArgDefine.SITE_CONF_STAGETAG)
    private String updateStageTag;

    public Updater() {
    }

    public String getUpdateStageTag() {
        return updateStageTag;
    }

    public void setUpdateStageTag(String updateStageTag) {
        this.updateStageTag = updateStageTag;
    }
}
