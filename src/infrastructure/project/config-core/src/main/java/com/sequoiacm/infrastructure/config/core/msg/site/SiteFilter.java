package com.sequoiacm.infrastructure.config.core.msg.site;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.SITE)
public class SiteFilter implements ConfigFilter {

    @JsonProperty(ScmRestArgDefine.SITE_CONF_SITENAME)
    private String siteName;

    public SiteFilter() {
    }

    public SiteFilter(String siteName) {
        this.siteName = siteName;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SiteFilter that = (SiteFilter) o;
        return Objects.equals(siteName, that.siteName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteName);
    }

    @Override
    public String toString() {
        return "SiteFilter{" + "siteName='" + siteName + '\'' + '}';
    }
}
