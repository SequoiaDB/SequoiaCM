package com.sequoiacm.infrastructure.config.core.msg.bucket;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.Config;

import java.util.Map;

public class BucketConfig implements Config {
    private String name;
    private long id;
    private String fileTable;
    private long createTime;
    private String createUser;
    private String workspace;
    private String versionStatus;
    private Map<String, String> customTag;
    private long updateTime;
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
    public BSONObject toBSONObject() {
        BasicBSONObject ret = new BasicBSONObject();
        ret.put(FieldName.Bucket.NAME, name);
        ret.put(FieldName.Bucket.ID, id);
        ret.put(FieldName.Bucket.WORKSPACE, workspace);
        ret.put(FieldName.Bucket.CREATE_TIME, createTime);
        ret.put(FieldName.Bucket.CREATE_USER, createUser);
        ret.put(FieldName.Bucket.FILE_TABLE, fileTable);
        ret.put(FieldName.Bucket.VERSION_STATUS, versionStatus);
        ret.put(FieldName.Bucket.CUSTOM_TAG, customTag);
        ret.put(FieldName.Bucket.UPDATE_TIME, updateTime);
        ret.put(FieldName.Bucket.UPDATE_USER, updateUser);
        return ret;
    }

    @Override
    public String toString() {
        return "BucketConfig{" + "name='" + name + '\'' + ", id=" + id + ", fileTable='" + fileTable
                + '\'' + ", createTime=" + createTime + ", createUser='" + createUser + '\''
                + ", workspace='" + workspace + '\'' + ", versionStatus='" + versionStatus + '\''
                + ", customTag=" + customTag + ", updateTime=" + updateTime + ", updateUser='"
                + updateUser + '\'' + '}';
    }
}
