package com.sequoiacm.om.omserver.module;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmFileBasic {
    @JsonProperty("name")
    private String name;

    @JsonProperty("user")
    private String user;

    @JsonProperty("mime_type")
    private String mimeType;

    @JsonProperty("id")
    private String id;

    @JsonProperty("size")
    private long size;

    @JsonProperty("major_version")
    private int majorVersion;

    @JsonProperty("minor_version")
    private int minorVersion;

    @JsonProperty("create_time")
    private Date createTime;

    @JsonProperty("update_time")
    private Date updateTime;

    @JsonProperty("delete_marker")
    private boolean isDeleteMarker;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public boolean isDeleteMarker() {
        return isDeleteMarker;
    }

    public void setDeleteMarker(boolean deleteMarker) {
        isDeleteMarker = deleteMarker;
    }
}
