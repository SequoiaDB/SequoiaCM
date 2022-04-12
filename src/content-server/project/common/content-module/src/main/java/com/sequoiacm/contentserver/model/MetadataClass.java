package com.sequoiacm.contentserver.model;

import java.util.List;


public class MetadataClass {

    private String id;
    private String name;
    private String description;
    private String createUser;
    private long createTime;
    private String updateUser;
    private long updateTime;
    
    private List<MetadataAttr> attrList;
    
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
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
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
    public String getUpdateUser() {
        return updateUser;
    }
    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }
    public long getUpdateTime() {
        return updateTime;
    }
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
    public List<MetadataAttr> getAttrList() {
        return attrList;
    }
    public void setAttrList(List<MetadataAttr> attrList) {
        this.attrList = attrList;
    }
}
