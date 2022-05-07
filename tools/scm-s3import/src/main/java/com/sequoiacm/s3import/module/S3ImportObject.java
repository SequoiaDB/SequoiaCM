package com.sequoiacm.s3import.module;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;

public class S3ImportObject {

    private String bucket;
    private String key;
    private String eTag;
    private long size;
    private Date lastModified;
    private boolean withVersion;
    private boolean isCompleted;
    private List<S3VersionSummary> versionSummaryList;

    private boolean hasDeleteMarker;

    public S3ImportObject(String bucket, String key, boolean withVersion) {
        this.withVersion = withVersion;
        this.bucket = bucket;
        this.key = key;
    }

    public S3ImportObject(S3ObjectSummary summary) {
        this.isCompleted = true;
        this.withVersion = false;
        this.bucket = summary.getBucketName();
        this.key = summary.getKey();
        this.eTag = summary.getETag();
        this.lastModified = summary.getLastModified();
        this.size = summary.getSize();
    }

    public boolean isWithVersion() {
        return withVersion;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public List<S3VersionSummary> getVersionSummaryList() {
        return versionSummaryList;
    }

    public void setVersionSummaryList(List<S3VersionSummary> versionSummaryList) {
        this.versionSummaryList = versionSummaryList;
    }

    public void addVersionSummary(S3VersionSummary versionSummary) {
        if (this.versionSummaryList == null) {
            this.versionSummaryList = new ArrayList<>();
        }
        this.versionSummaryList.add(versionSummary);
    }

    public boolean isHasDeleteMarker() {
        return hasDeleteMarker;
    }

    public void setHasDeleteMarker(boolean hasDeleteMarker) {
        this.hasDeleteMarker = hasDeleteMarker;
    }

    public boolean isCompleted() {
        return isCompleted || versionSummaryList != null;
    }
}
