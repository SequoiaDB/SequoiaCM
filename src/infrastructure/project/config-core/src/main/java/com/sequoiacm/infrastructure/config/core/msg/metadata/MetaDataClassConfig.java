package com.sequoiacm.infrastructure.config.core.msg.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

import java.util.Objects;

public class MetaDataClassConfig {
    @JsonProperty(ScmRestArgDefine.META_DATA_WORKSPACE_NAME)
    private String wsName;

    @JsonProperty(FieldName.Class.FIELD_ID)
    private String id;

    @JsonProperty(FieldName.Class.FIELD_NAME)
    private String name;

    @JsonProperty(FieldName.Class.FIELD_DESCRIPTION)
    private String description = "";

    @JsonProperty(FieldName.Class.FIELD_INNER_CREATE_USER)
    private String createUser;

    @JsonProperty(FieldName.Class.FIELD_INNER_CREATE_TIME)
    private long createTime;

    @JsonProperty(FieldName.Class.FIELD_INNER_UPDATE_USER)
    private String updateUser;

    @JsonProperty(FieldName.Class.FIELD_INNER_UPDATE_TIME)
    private long updateTime;

    public MetaDataClassConfig(BSONObject object) {
        wsName = BsonUtils.getString(object, ScmRestArgDefine.META_DATA_WORKSPACE_NAME);
        id = BsonUtils.getString(object, FieldName.Class.FIELD_ID);
        name = BsonUtils.getString(object, FieldName.Class.FIELD_NAME);
        description = BsonUtils.getStringOrElse(object, FieldName.Class.FIELD_DESCRIPTION, "");
        createUser = BsonUtils.getString(object, FieldName.Class.FIELD_INNER_CREATE_USER);
        createTime = BsonUtils
                .getNumberOrElse(object, FieldName.Class.FIELD_INNER_CREATE_TIME, 0)
                .longValue();
        updateUser = BsonUtils.getString(object, FieldName.Class.FIELD_INNER_UPDATE_USER);
        updateTime = BsonUtils
                .getNumberOrElse(object, FieldName.Class.FIELD_INNER_UPDATE_TIME, 0)
                .longValue();
    }

    public MetaDataClassConfig() {
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
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

    public BSONObject toRecord() {
        BasicBSONObject record = new BasicBSONObject();
        record.put(FieldName.Class.FIELD_DESCRIPTION, description);
        record.put(FieldName.Class.FIELD_ID, id);
        record.put(FieldName.Class.FIELD_INNER_CREATE_TIME, createTime);
        record.put(FieldName.Class.FIELD_INNER_CREATE_USER, createUser);
        record.put(FieldName.Class.FIELD_INNER_UPDATE_TIME, updateTime);
        record.put(FieldName.Class.FIELD_INNER_UPDATE_USER, updateUser);
        record.put(FieldName.Class.FIELD_NAME, name);
        return record;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetaDataClassConfig that = (MetaDataClassConfig) o;
        return createTime == that.createTime && updateTime == that.updateTime && Objects.equals(wsName, that.wsName) && Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(createUser, that.createUser) && Objects.equals(updateUser, that.updateUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wsName, id, name, description, createUser, createTime, updateUser, updateTime);
    }
}
