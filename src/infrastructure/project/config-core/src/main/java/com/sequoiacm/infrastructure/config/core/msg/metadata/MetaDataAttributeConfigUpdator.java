package com.sequoiacm.infrastructure.config.core.msg.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class MetaDataAttributeConfigUpdator {
    // new properties
    private String displayName;
    private String description;
    private BSONObject checkRule;
    private Boolean isRequire;
    private String updateUser;

    // target
    private String wsName;
    private String attributeId;

    public MetaDataAttributeConfigUpdator(String wsName, String attributeId, String updateUser) {
        this.wsName = wsName;
        this.updateUser = updateUser;
        this.attributeId = attributeId;
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

    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(FieldName.Attribute.FIELD_REQUIRED, isRequire);
        obj.put(FieldName.Attribute.FIELD_CHECK_RULE, checkRule);
        obj.put(FieldName.Attribute.FIELD_DESCRIPTION, description);
        obj.put(FieldName.Attribute.FIELD_DISPLAY_NAME, displayName);
        obj.put(ScmRestArgDefine.META_DATA_WORKSPACE_NAME, wsName);
        obj.put(ScmRestArgDefine.META_DATA_ATTRIBUTE_ID, attributeId);
        obj.put(ScmRestArgDefine.META_DATA_UPDATE_USER, updateUser);
        return obj;
    }

}
