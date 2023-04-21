package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OmBucketCreateInfo {

    @JsonProperty("bucket_names")
    private List<String> bucketNames;

    @JsonProperty("version_status")
    private String versionStatus;

    @JsonProperty("workspace")
    private String workspace;

    @JsonProperty("enable_quota")
    private Boolean enableQuota;

    @JsonProperty("max_objects")
    private Long maxObjects;

    @JsonProperty("max_size")
    private String maxSize;

    public List<String> getBucketNames() {
        return bucketNames;
    }

    public void setBucketNames(List<String> bucketNames) {
        this.bucketNames = bucketNames;
    }

    public String getVersionStatus() {
        return versionStatus;
    }

    public void setVersionStatus(String versionStatus) {
        this.versionStatus = versionStatus;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public Boolean getEnableQuota() {
        return enableQuota;
    }

    public void setEnableQuota(Boolean enableQuota) {
        this.enableQuota = enableQuota;
    }

    public Long getMaxObjects() {
        return maxObjects;
    }

    public void setMaxObjects(Long maxObjects) {
        this.maxObjects = maxObjects;
    }

    public String getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(String maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public String toString() {
        return "OmBucketCreateInfo{" + "bucketNames=" + bucketNames + ", versionStatus='"
                + versionStatus + '\'' + ", workspace='" + workspace + '\'' + ", enableQuota="
                + enableQuota + ", maxObjects=" + maxObjects + ", maxSize='" + maxSize + '\'' + '}';
    }
}
