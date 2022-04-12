package com.sequoiacm.s3.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.s3.utils.DataFormatUtils;

public class Bucket {
    public static final String BUCKET_NAME = "Name";
    public static final String BUCKET_CREATETIME = "CreationDate";

    @JsonProperty(BUCKET_NAME)
    private String bucketName;
    @JsonProperty(BUCKET_CREATETIME)
    private String createDate;
    @JsonIgnore
    private String user;
    @JsonIgnore
    private String region;

    public Bucket() {
    }

    public Bucket(String bucketName, long createDateL, String user, String region) {
        this.bucketName = bucketName;
        this.createDate = DataFormatUtils.formatDate(createDateL);
        this.region = region;
        this.user = user;
    }

    @Override
    public String toString() {
        return "Bucket{" + "bucketName='" + bucketName + '\'' + ", createDate='" + createDate + '\''
                + ", user='" + user + '\'' + ", region='" + region + '\'' + '}';
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
