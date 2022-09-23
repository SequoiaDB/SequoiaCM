package com.sequoiacm.om.omserver.module;

import java.util.Date;
import java.util.List;

import org.bson.BSONObject;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmWorkspaceDetail extends OmWorkspaceBasicInfo {
    @JsonProperty("update_user")
    private String updateUser;

    @JsonProperty("update_time")
    private Date updateTime;

    @JsonProperty("data_locations")
    private List<OmWorkspaceDataLocation> dataLocations;

    @JsonProperty("meta_options")
    private BSONObject metaOption;

    @JsonProperty("enable_directory")
    private Boolean enableDirectory;

    @JsonProperty("site_cache_strategy")
    private String siteCacheStrategy;

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public List<OmWorkspaceDataLocation> getDataLocations() {
        return dataLocations;
    }

    public void setDataLocations(List<OmWorkspaceDataLocation> dataLocations) {
        this.dataLocations = dataLocations;
    }

    public BSONObject getMetaOption() {
        return metaOption;
    }

    public void setMetaOption(BSONObject metaOption) {
        this.metaOption = metaOption;
    }

    public Boolean getEnableDirectory() {
        return enableDirectory;
    }

    public void setEnableDirectory(Boolean enableDirectory) {
        this.enableDirectory = enableDirectory;
    }

    public String getSiteCacheStrategy() {
        return siteCacheStrategy;
    }

    public void setSiteCacheStrategy(String siteCacheStrategy) {
        this.siteCacheStrategy = siteCacheStrategy;
    }
}
