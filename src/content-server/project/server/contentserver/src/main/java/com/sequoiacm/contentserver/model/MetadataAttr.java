package com.sequoiacm.contentserver.model;

import org.bson.BSONObject;

import com.sequoiacm.common.AttributeType;

public class MetadataAttr {

    private String id;
    private String name;
    private String displayName;
    private String description;
    private AttributeType type;
    private BSONObject checkRule;
    private boolean required;
    private String createUser;
    private long createTime;
    private String updateUser;
    private long updateTime;
    
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
    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public AttributeType getType() {
        return type;
    }
    public void setType(AttributeType type) {
        this.type = type;
    }
    public BSONObject getCheckRule() {
        return checkRule;
    }
    public void setCheckRule(BSONObject checkRule) {
        this.checkRule = checkRule;
    }
    public boolean isRequired() {
        return required;
    }
    public void setRequired(boolean required) {
        this.required = required;
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
}
