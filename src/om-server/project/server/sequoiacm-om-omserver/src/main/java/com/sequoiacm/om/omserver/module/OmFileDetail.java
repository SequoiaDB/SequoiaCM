package com.sequoiacm.om.omserver.module;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmFileDetail extends OmFileBasic {
    @JsonProperty("author")
    private String author;

    @JsonProperty("title")
    private String title;

    @JsonProperty("update_user")
    private String updateUser;

    @JsonProperty("directory_id")
    private String directoryId;

    @JsonProperty("directory_path")
    private String directoryPath;

    @JsonProperty("bucket_id")
    private Long bucketId;

    @JsonProperty("batch_id")
    private String batchId;

    @JsonProperty("batch_name")
    private String batchName;

    @JsonProperty("md5")
    private String md5;

    @JsonProperty("mime_type")
    private String mimeType;

    @JsonProperty("sites")
    private List<OmFileDataSiteInfo> sites;

    @JsonProperty("data_id")
    private String dataId;

    @JsonProperty("data_create_time")
    private Date dataCreateTime;

    @JsonProperty("tags")
    private Set<String> tags;

    @JsonProperty("class_id")
    private String classId;

    @JsonProperty("class_name")
    private String className;

    @JsonProperty("class_properties")
    private Map<String, Object> classProperties;

    public OmFileDetail() {
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getDirectoryId() {
        return directoryId;
    }

    public void setDirectoryId(String directoryId) {
        this.directoryId = directoryId;
    }

    public Long getBucketId() {
        return bucketId;
    }

    public void setBucketId(Long bucketId) {
        this.bucketId = bucketId;
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

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public Date getDataCreateTime() {
        return dataCreateTime;
    }

    public void setDataCreateTime(Date dataCreateTime) {
        this.dataCreateTime = dataCreateTime;
    }

    public List<OmFileDataSiteInfo> getSites() {
        return sites;
    }

    public void setSites(List<OmFileDataSiteInfo> sites) {
        this.sites = sites;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
