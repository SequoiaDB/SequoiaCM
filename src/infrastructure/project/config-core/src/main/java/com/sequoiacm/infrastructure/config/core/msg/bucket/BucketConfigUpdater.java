package com.sequoiacm.infrastructure.config.core.msg.bucket;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class BucketConfigUpdater implements ConfigUpdator {
    // target
    private String bucketName;

    // new version status
    private String versionStatus;
    private String updateUser;

    public BucketConfigUpdater(String bucketName, String versionStatus, String updateUser) {
        this.bucketName = bucketName;
        this.versionStatus = versionStatus;
        this.updateUser = updateUser;
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

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    @Override
    public String toString() {
        return "BucketConfigUpdater{" + "bucketName='" + bucketName + '\'' + ", versionStatus='"
                + versionStatus + '\'' + ", updateUser='" + updateUser + '\'' + '}';
    }

    @Override
    public BSONObject toBSONObject() {
        return new BasicBSONObject().append(FieldName.Bucket.NAME, bucketName)
                .append(FieldName.Bucket.VERSION_STATUS, versionStatus)
                .append(FieldName.Bucket.UPDATE_USER, updateUser);
    }

    public String getUpdateUser() {
        return updateUser;
    }
}
