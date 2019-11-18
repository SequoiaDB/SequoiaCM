package com.sequoiacm.om.omserver.module;

import org.bson.BSONObject;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author huangqiaohui
 *
 */
public class OmWorkspaceDataLocation {
    @JsonProperty("site_name")
    private String siteName;
    @JsonProperty("site_type")
    private String siteType;
    @JsonProperty("options")
    private BSONObject options;

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getSiteType() {
        return siteType;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public BSONObject getOptions() {
        return options;
    }

    public void setOptions(BSONObject options) {
        this.options = options;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        OmWorkspaceDataLocation other = (OmWorkspaceDataLocation) obj;
        if (options == null) {
            if (other.options != null) {
                return false;
            }
        }
        else if (!options.equals(other.options)) {
            return false;
        }
        if (siteName == null) {
            if (other.siteName != null) {
                return false;
            }
        }
        else if (!siteName.equals(other.siteName)) {
            return false;
        }
        if (siteType == null) {
            if (other.siteType != null) {
                return false;
            }
        }
        else if (!siteType.equals(other.siteType)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ScmWorkspaceDataLocation [siteName=" + siteName + ", siteType=" + siteType
                + ", options=" + options + "]";
    }

}
