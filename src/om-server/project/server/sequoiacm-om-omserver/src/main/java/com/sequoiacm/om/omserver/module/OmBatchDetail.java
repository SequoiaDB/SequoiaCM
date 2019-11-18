package com.sequoiacm.om.omserver.module;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.exception.ScmException;

public class OmBatchDetail extends OmBatchBasic {
    @JsonProperty("class_id")
    private String classId;

    @JsonProperty("class_properties")
    private Map<String, Object> classProperties;

    @JsonProperty("tags")
    private Set<String> tags;

    @JsonProperty("crete_user")
    private String createUser;

    @JsonProperty("update_user")
    private String updateUser;

    @JsonProperty("update_time")
    private Date updateTime;

    @JsonProperty("files")
    private List<OmFileBasic> files;

    public OmBatchDetail() {
    }

    public OmBatchDetail(ScmBatch batch) throws ScmException {
        this.setClassId(batch.getClassId() == null ? "" : batch.getClassId().get());
        this.setClassProperties(batch.getClassProperties() == null ? new HashMap<String, Object>()
                : batch.getClassProperties().toMap());
        this.setCreateTime(batch.getCreateTime());
        this.setCreateUser(batch.getCreateUser());
        this.setId(batch.getId().get());
        this.setName(batch.getName());
        this.setTags(batch.getTags() == null ? new HashSet<String>() : batch.getTags().toSet());
        this.setUpdateTime(batch.getUpdateTime());
        this.setUpdateUser(batch.getUpdateUser());
        List<ScmFile> files = batch.listFiles();
        List<OmFileBasic> fileBasicList = new ArrayList<>();
        for (ScmFile file : files) {
            OmFileBasic fileBasic = new OmFileBasic();
            fileBasic.setCreateTime(file.getCreateTime());
            fileBasic.setId(file.getFileId().get());
            fileBasic.setMajorVersion(file.getMajorVersion());
            fileBasic.setMimeType(file.getMimeType());
            fileBasic.setMinorVersion(file.getMinorVersion());
            fileBasic.setName(file.getFileName());
            fileBasic.setUser(file.getUser());
            fileBasicList.add(fileBasic);
        }
        this.setFiles(fileBasicList);
        this.setFileCount(fileBasicList.size());
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public Map<String, Object> getClassProperties() {
        return classProperties;
    }

    public void setClassProperties(Map<String, Object> classProperties) {
        this.classProperties = classProperties;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public List<OmFileBasic> getFiles() {
        return files;
    }

    public void setFiles(List<OmFileBasic> files) {
        this.files = files;
    }

}
