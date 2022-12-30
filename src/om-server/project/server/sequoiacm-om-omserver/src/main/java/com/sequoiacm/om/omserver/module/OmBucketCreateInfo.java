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

    @Override
    public String toString() {
        return "OmBucketCreateInfo{" + "bucketNames=" + bucketNames + ", versionStatus='"
                + versionStatus + '\'' + ", workspace='" + workspace + '\'' + '}';
    }
}
