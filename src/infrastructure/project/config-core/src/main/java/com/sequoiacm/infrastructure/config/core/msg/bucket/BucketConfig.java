package com.sequoiacm.infrastructure.config.core.msg.bucket;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.Config;

import java.util.Map;
import java.util.Objects;

@BusinessType(ScmBusinessTypeDefine.BUCKET)
public class BucketConfig implements Config {

    @JsonProperty(FieldName.Bucket.NAME)
    private String name;

    @JsonProperty(FieldName.Bucket.ID)
    private long id;

    @JsonProperty(FieldName.Bucket.FILE_TABLE)
    private String fileTable;

    @JsonProperty(FieldName.Bucket.CREATE_TIME)
    private long createTime;

    @JsonProperty(FieldName.Bucket.CREATE_USER)
    private String createUser;

    @JsonProperty(FieldName.Bucket.WORKSPACE)
    private String workspace;

    @JsonProperty(FieldName.Bucket.VERSION_STATUS)
    private String versionStatus;

    @JsonProperty(FieldName.Bucket.CUSTOM_TAG)
    private Map<String, String> customTag;

    @JsonProperty(FieldName.Bucket.UPDATE_TIME)
    private long updateTime;

    @JsonProperty(FieldName.Bucket.UPDATE_USER)
    private String updateUser;

    public BucketConfig() {
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFileTable() {
        return fileTable;
    }

    public void setFileTable(String fileTable) {
        this.fileTable = fileTable;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
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

    @Override
    public String toString() {
        return "BucketConfig{" + "name='" + name + '\'' + ", id=" + id + ", fileTable='" + fileTable
                + '\'' + ", createTime=" + createTime + ", createUser='" + createUser + '\''
                + ", workspace='" + workspace + '\'' + ", versionStatus='" + versionStatus + '\''
                + ", customTag=" + customTag + ", updateTime=" + updateTime + ", updateUser='"
                + updateUser + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BucketConfig that = (BucketConfig) o;
        return id == that.id && createTime == that.createTime && updateTime == that.updateTime
                && Objects.equals(name, that.name) && Objects.equals(fileTable, that.fileTable)
                && Objects.equals(createUser, that.createUser)
                && Objects.equals(workspace, that.workspace)
                && Objects.equals(versionStatus, that.versionStatus)
                && Objects.equals(customTag, that.customTag)
                && Objects.equals(updateUser, that.updateUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, fileTable, createTime, createUser, workspace, versionStatus,
                customTag, updateTime, updateUser);
    }

    @Override
    public String getBusinessName() {
        return name;
    }
}
