package com.sequoiacm.s3.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Bucket {
    public static final String BUCKET_NAME = "Name";
    public static final String BUCKET_CREATETIME = "CreationDate";

    @JsonProperty(BUCKET_NAME)
    private String bucketName;
    @JsonProperty(BUCKET_CREATETIME)
    private String createDate;
    @JsonIgnore
    private String workspace;
    @JsonIgnore
    private String bucketDir;

    @JsonIgnore
    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBucketDir() {
        return bucketDir;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getRegion() {
        return getWorkspace();
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public void setBucketDir(String bucketDir) {
        this.bucketDir = bucketDir;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getBucketName() {
        return this.bucketName;
    }

}
