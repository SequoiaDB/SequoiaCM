package com.sequoiacm.infrastructure.config.core.msg.bucket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdater;

import java.util.Map;
import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.BUCKET)
public class BucketConfigUpdater implements ConfigUpdater {
    // target
    @JsonProperty(FieldName.Bucket.NAME)
    private String bucketName;

    // new version status
    @JsonProperty(FieldName.Bucket.VERSION_STATUS)
    private String versionStatus;

    @JsonProperty(FieldName.Bucket.CUSTOM_TAG)
    private Map<String, String> customTag;

    @JsonProperty(FieldName.Bucket.UPDATE_USER)
    private String updateUser;

    public BucketConfigUpdater(String bucketName) {
        this.bucketName = bucketName;
    }

    public BucketConfigUpdater() {
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getVersionStatus() {
        return versionStatus;
    }

    public void setVersionStatus(String versionStatus) {
        this.versionStatus = versionStatus;
    }

    public Map<String, String> getCustomTag() {
        return customTag;
    }

    public void setCustomTag(Map<String, String> customTag) {
        this.customTag = customTag;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    @Override
    public String toString() {
        return "BucketConfigUpdater{" + "bucketName='" + bucketName + '\'' + ", versionStatus='"
                + versionStatus + '\'' + ", customTag=" + customTag + ", updateUser='" + updateUser
                + '\'' + '}';
    }

    public String getUpdateUser() {
        return updateUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BucketConfigUpdater that = (BucketConfigUpdater) o;
        return Objects.equals(bucketName, that.bucketName)
                && Objects.equals(versionStatus, that.versionStatus)
                && Objects.equals(customTag, that.customTag)
                && Objects.equals(updateUser, that.updateUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketName, versionStatus, customTag, updateUser);
    }
}
