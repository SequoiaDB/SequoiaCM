package com.sequoiacm.infrastructure.config.core.msg.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

import java.util.Objects;

public class MetaDataAttributeConfigUpdator {
    // new properties
    @JsonProperty(FieldName.Attribute.FIELD_DISPLAY_NAME)
    private String displayName;

    @JsonProperty(FieldName.Attribute.FIELD_DESCRIPTION)
    private String description;

    @JsonProperty(FieldName.Attribute.FIELD_CHECK_RULE)
    private BSONObject checkRule;

    @JsonProperty(FieldName.Attribute.FIELD_REQUIRED)
    private Boolean isRequire;

    @JsonProperty(ScmRestArgDefine.META_DATA_UPDATE_USER)
    private String updateUser;

    @JsonProperty(ScmRestArgDefine.META_DATA_WORKSPACE_NAME)
    // target
    private String wsName;

    @JsonProperty(ScmRestArgDefine.META_DATA_ATTRIBUTE_ID)
    private String attributeId;

    public MetaDataAttributeConfigUpdator(String wsName, String attributeId, String updateUser) {
        this.wsName = wsName;
        this.updateUser = updateUser;
        this.attributeId = attributeId;
    }

    public MetaDataAttributeConfigUpdator() {
    }

    public MetaDataAttributeConfigUpdator(BSONObject obj) {
        wsName = BsonUtils.getString(obj, ScmRestArgDefine.META_DATA_WORKSPACE_NAME);
        attributeId = BsonUtils.getString(obj, ScmRestArgDefine.META_DATA_ATTRIBUTE_ID);
        updateUser = BsonUtils.getString(obj, ScmRestArgDefine.META_DATA_UPDATE_USER);
        displayName = BsonUtils.getString(obj, FieldName.Attribute.FIELD_DISPLAY_NAME);
        description = BsonUtils.getString(obj, FieldName.Attribute.FIELD_DESCRIPTION);
        checkRule = BsonUtils.getBSON(obj, FieldName.Attribute.FIELD_CHECK_RULE);
        isRequire = BsonUtils.getBoolean(obj, FieldName.Attribute.FIELD_REQUIRED);
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public String getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(String attributeId) {
        this.attributeId = attributeId;
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

    public BSONObject getCheckRule() {
        return checkRule;
    }

    public void setCheckRule(BSONObject checkRule) {
        this.checkRule = checkRule;
    }

    public Boolean getIsRequire() {
        return isRequire;
    }

    public void setIsRequire(Boolean isRequire) {
        this.isRequire = isRequire;
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public boolean isNeedUpdate() {
        if (description != null) {
            return true;
        }

        if (checkRule != null) {
            return true;
        }

        if (displayName != null) {
            return true;
        }

        if (isRequire != null) {
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetaDataAttributeConfigUpdator that = (MetaDataAttributeConfigUpdator) o;
        return Objects.equals(displayName, that.displayName) && Objects.equals(description, that.description) && Objects.equals(checkRule, that.checkRule) && Objects.equals(isRequire, that.isRequire) && Objects.equals(updateUser, that.updateUser) && Objects.equals(wsName, that.wsName) && Objects.equals(attributeId, that.attributeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, description, checkRule, isRequire, updateUser, wsName, attributeId);
    }
}
