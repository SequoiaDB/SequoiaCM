package com.sequoiacm.infrastructure.config.core.msg.site;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;

import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.SITE)
public class SiteNotifyOption implements NotifyOption {
    @JsonProperty(ScmRestArgDefine.SITE_CONF_SITENAME)
    private String siteName;

    @JsonProperty(ScmRestArgDefine.SITE_CONF_SITEVERSION)
    private Integer version;

    public SiteNotifyOption(String siteName, Integer version) {
        this.siteName = siteName;
        this.version = version;
    }

    public SiteNotifyOption() {
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public Version getBusinessVersion() {

        return new Version(ScmBusinessTypeDefine.SITE, siteName, version);
    }

    @Override
    public String getBusinessName() {
        return siteName;
    }

    @Override
    public String toString() {
        return "SiteNotifyOption{" + "siteName='" + siteName + '\'' + ", version=" + version + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SiteNotifyOption that = (SiteNotifyOption) o;
        return Objects.equals(siteName, that.siteName) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteName, version);
    }
}
