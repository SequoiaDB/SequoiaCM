package com.sequoiacm.s3.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.core.S3ObjectMeta;
import com.sequoiacm.s3.exception.S3ServerException;

public class ListObjVersion {
    @JsonProperty("Key")
    private String key;
    @JsonProperty("VersionId")
    private String versionId;
    @JsonProperty("IsLatest")
    private Boolean isLatest;
    @JsonProperty("LastModified")
    private String lastModified;
    @JsonProperty("Owner")
    private Owner owner;

    @JsonProperty("ETag")
    private String eTag;
    @JsonProperty("Size")
    private long size;

    public ListObjVersion(String key, String versionId, Boolean isLatest, String lastModified,
            String user, String eTag, long size) {
        this.key = key;
        this.versionId = versionId;
        this.isLatest = isLatest;
        this.lastModified = lastModified;
        this.owner = new Owner(user, user);
        this.eTag = eTag;
        this.size = size;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setIsLatest(Boolean latest) {
        this.isLatest = latest;
    }

    public Boolean getIsLatest() {
        return this.isLatest;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public Owner getOwner() {
        return owner;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    public String geteTag() {
        return eTag;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }
}
