package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.element.metadata.ScmAttrRule;
import com.sequoiacm.common.AttributeType;

import java.util.Date;

public class OmAttributeBasicInfo {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private AttributeType type;

    @JsonProperty("check_rule")
    private ScmAttrRule checkRule;

    @JsonProperty("required")
    private boolean required;

    @JsonProperty("create_user")
    private String createUser;

    @JsonProperty("create_time")
    private Date createTime;

    @JsonProperty("update_user")
    private String updateUser;

    @JsonProperty("update_time")
    private Date updateTime;

    public OmAttributeBasicInfo(ScmAttribute scmAttribute) {
        this.id = scmAttribute.getId().get();
        this.name = scmAttribute.getName();
        this.displayName = scmAttribute.getDisplayName();
        this.description = scmAttribute.getDescription();
        this.type = scmAttribute.getType();
        this.checkRule = scmAttribute.getCheckRule();
        this.required = scmAttribute.isRequired();
        this.createUser = scmAttribute.getCreateUser();
        this.createTime = scmAttribute.getCreateTime();
        this.updateUser = scmAttribute.getUpdateUser();
        this.updateTime = scmAttribute.getUpdateTime();
    }

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

    public ScmAttrRule getCheckRule() {
        return checkRule;
    }

    public void setCheckRule(ScmAttrRule checkRule) {
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
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
}
