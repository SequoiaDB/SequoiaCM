package com.sequoiacm.fulltext.server.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.common.FieldName;

public class ScmFileBasicInfo {
    @JsonProperty(FieldName.FIELD_CLFILE_ID)
    private String id;
    @JsonProperty(FieldName.FIELD_CLFILE_NAME)
    private String name;
    @JsonProperty(FieldName.FIELD_CLFILE_INNER_USER)
    private String createUser;
    @JsonProperty(FieldName.FIELD_CLFILE_INNER_CREATE_TIME)
    private long createTime;
    @JsonProperty(FieldName.FIELD_CLREL_FILE_MIME_TYPE)
    private String mimeType;
    @JsonProperty(FieldName.FIELD_CLFILE_MAJOR_VERSION)
    private int majorVersion;
    @JsonProperty(FieldName.FIELD_CLFILE_MINOR_VERSION)
    private int minorVersion;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
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

}
