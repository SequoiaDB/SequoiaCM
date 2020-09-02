package com.sequoiacm.s3.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.s3.core.ObjectMeta;
import com.sequoiacm.s3.exception.S3ServerException;

public class Version {
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
    @JsonIgnore
    private Boolean noVersionFlag;

    @JsonProperty("ETag")
    private String eTag;
    @JsonProperty("Size")
    private long size;

    public Version(ListObjRecord obj) throws S3ServerException {
        key = obj.getKey();
        lastModified = obj.getLastModified();
        versionId = ObjectMeta.NULL_VERSION_ID;
        eTag = obj.geteTag();
        size = obj.getSize();
        isLatest = true;
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

    public void setNoVersionFlag(Boolean noVersionFlag) {
        this.noVersionFlag = noVersionFlag;
    }

    public Boolean getNoVersionFlag() {
        return noVersionFlag;
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
