package com.sequoiacm.infrastructure.config.core.msg.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

import java.util.Objects;

public class MetaDataAttributeConfig {

    @JsonProperty(ScmRestArgDefine.META_DATA_WORKSPACE_NAME)
    private String wsName;

    @JsonProperty(FieldName.Attribute.FIELD_ID)
    private String id;

    @JsonProperty(FieldName.Attribute.FIELD_NAME)
    private String name;

    @JsonProperty(FieldName.Attribute.FIELD_DISPLAY_NAME)
    private String displayName = "";

    @JsonProperty(FieldName.Attribute.FIELD_DESCRIPTION)
    private String description = "";

    @JsonProperty(FieldName.Attribute.FIELD_TYPE)
    private String type;

    @JsonProperty(FieldName.Attribute.FIELD_CHECK_RULE)
    private BSONObject checkRule;

    @JsonProperty(FieldName.Attribute.FIELD_REQUIRED)
    private boolean required;

    @JsonProperty(FieldName.Attribute.FIELD_INNER_CREATE_USER)
    private String createUser;

    @JsonProperty(FieldName.Attribute.FIELD_INNER_CREATE_TIME)
    private long createTime;

    @JsonProperty(FieldName.Attribute.FIELD_INNER_UPDATE_USER)
    private String updateUser;

    @JsonProperty(FieldName.Attribute.FIELD_INNER_UPDATE_TIME)
    private long updateTime;

    public MetaDataAttributeConfig() {
    }

    public MetaDataAttributeConfig(BSONObject config) {
        wsName = BsonUtils.getString(config, ScmRestArgDefine.META_DATA_WORKSPACE_NAME);
        id = BsonUtils.getString(config, FieldName.Attribute.FIELD_ID);
        checkRule = BsonUtils.getBSON(config, FieldName.Attribute.FIELD_CHECK_RULE);
        description = BsonUtils.getStringOrElse(config, FieldName.Attribute.FIELD_DESCRIPTION,
                "");
        displayName = BsonUtils.getStringOrElse(config, FieldName.Attribute.FIELD_DISPLAY_NAME,
                "");
        createTime = BsonUtils
                .getNumberOrElse(config, FieldName.Attribute.FIELD_INNER_CREATE_TIME, 0)
                .longValue();
        createUser = BsonUtils.getString(config, FieldName.Attribute.FIELD_INNER_CREATE_USER);
        updateTime = BsonUtils
                .getNumberOrElse(config, FieldName.Attribute.FIELD_INNER_UPDATE_TIME, 0)
                .longValue();
        updateUser = BsonUtils.getString(config, FieldName.Attribute.FIELD_INNER_UPDATE_USER);
        name = BsonUtils.getString(config, FieldName.Attribute.FIELD_NAME);
        required = BsonUtils.getBooleanOrElse(config, FieldName.Attribute.FIELD_REQUIRED,
                false);
        type = BsonUtils.getString(config, FieldName.Attribute.FIELD_TYPE);
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
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

    public BSONObject toRecord() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(FieldName.Attribute.FIELD_CHECK_RULE, checkRule);
        obj.put(FieldName.Attribute.FIELD_DESCRIPTION, description);
        obj.put(FieldName.Attribute.FIELD_DISPLAY_NAME, displayName);
        obj.put(FieldName.Attribute.FIELD_ID, id);
        obj.put(FieldName.Attribute.FIELD_INNER_CREATE_TIME, createTime);
        obj.put(FieldName.Attribute.FIELD_INNER_CREATE_USER, createUser);
        obj.put(FieldName.Attribute.FIELD_INNER_UPDATE_TIME, updateTime);
        obj.put(FieldName.Attribute.FIELD_INNER_UPDATE_USER, updateUser);
        obj.put(FieldName.Attribute.FIELD_NAME, name);
        obj.put(FieldName.Attribute.FIELD_REQUIRED, required);
        obj.put(FieldName.Attribute.FIELD_TYPE, type);
        return obj;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MetaDataAttributeConfig that = (MetaDataAttributeConfig) o;
        return required == that.required && createTime == that.createTime
                && updateTime == that.updateTime && Objects.equals(wsName, that.wsName)
                && Objects.equals(id, that.id) && Objects.equals(name, that.name)
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(description, that.description) && Objects.equals(type, that.type)
                && Objects.equals(checkRule, that.checkRule)
                && Objects.equals(createUser, that.createUser)
                && Objects.equals(updateUser, that.updateUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wsName, id, name, displayName, description, type, checkRule, required,
                createUser, createTime, updateUser, updateTime);
    }
}
