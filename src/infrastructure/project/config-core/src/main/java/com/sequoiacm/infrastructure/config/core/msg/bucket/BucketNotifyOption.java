package com.sequoiacm.infrastructure.config.core.msg.bucket;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;

import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.BUCKET)
public class BucketNotifyOption implements NotifyOption {
    @JsonProperty(FieldName.Bucket.NAME)
    private String bucketName;

    @JsonProperty(ScmRestArgDefine.BUCKET_CONF_VERSION)
    private Integer version;

    @JsonProperty(ScmRestArgDefine.BUCKET_CONF_GLOBAL_VERSION)
    private int globalVersion;

    public BucketNotifyOption(String bucketName, Integer version,
            int globalVersion) {
        this.bucketName = bucketName;
        this.version = version;
        this.globalVersion = globalVersion;
    }

    public BucketNotifyOption() {
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public int getGlobalVersion() {
        return globalVersion;
    }

    public void setGlobalVersion(int globalVersion) {
        this.globalVersion = globalVersion;
    }

    @JsonIgnore
    @Override
    public String getBusinessName() {
        return bucketName;
    }

    @Override
    public Version getBusinessVersion() {
        return new Version(ScmBusinessTypeDefine.BUCKET, BucketConfigDefine.ALL_BUCKET_VERSION,
                globalVersion);
    }

    @Override
    public String toString() {
        return "BucketNotifyOption{" + "bucketName='" + bucketName + '\'' + ", version=" + version
                + ", globalVersion=" + globalVersion + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BucketNotifyOption that = (BucketNotifyOption) o;
        return globalVersion == that.globalVersion && Objects.equals(bucketName, that.bucketName)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketName, version, globalVersion);
    }
}
