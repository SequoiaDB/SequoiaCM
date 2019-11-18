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

    @JsonProperty("class_id")
    private String classId;

    @JsonProperty("directory_id")
    private String directoryId;

    @JsonProperty("class_properties")
    private Map<String, Object> classProperties;

    @JsonProperty("tags")
    private Set<String> tags;

    @JsonProperty("batch_id")
    private String batchId;

    @JsonProperty("data_id")
    private String dataId;

    @JsonProperty("data_create_time")
    private Date dataCreateTime;

    @JsonProperty("sites")
    private List<OmFileDataSiteInfo> sites;

    @JsonProperty("update_time")
    private Date updateTime;

    @JsonProperty("update_user")
    private String updateUser;

    @JsonProperty("size")
    private long size;

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

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

}
