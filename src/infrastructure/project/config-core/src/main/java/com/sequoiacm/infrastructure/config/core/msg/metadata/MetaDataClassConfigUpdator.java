package com.sequoiacm.infrastructure.config.core.msg.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

import java.util.Objects;

public class MetaDataClassConfigUpdator {

    @JsonProperty(ScmRestArgDefine.META_DATA_WORKSPACE_NAME)
    // update target
    private String wsName;

    @JsonProperty(ScmRestArgDefine.META_DATA_CLASS_ID)
    private String classId;

    // new property

    @JsonProperty(ScmRestArgDefine.META_DATA_ATTACH_ATTRUBUTE_ID)
    private String attachAttributeId;

    @JsonProperty(ScmRestArgDefine.META_DATA_DETTACH_ATTRUBUTE_ID)
    private String dettachAttributeId;

    @JsonProperty(FieldName.Class.FIELD_NAME)
    private String name;

    @JsonProperty(FieldName.Class.FIELD_DESCRIPTION)
    private String description;

    @JsonProperty(ScmRestArgDefine.META_DATA_UPDATE_USER)
    private String updateUser;

    public MetaDataClassConfigUpdator(String wsName, String classId, String updateUser) {
        this.updateUser = updateUser;
        this.wsName = wsName;
        this.classId = classId;
    }

    public MetaDataClassConfigUpdator() {
    }

    public MetaDataClassConfigUpdator(BSONObject obj) {
        attachAttributeId = BsonUtils.getString(obj,
                ScmRestArgDefine.META_DATA_ATTACH_ATTRUBUTE_ID);
        dettachAttributeId = BsonUtils.getString(obj,
                ScmRestArgDefine.META_DATA_DETTACH_ATTRUBUTE_ID);
        name = BsonUtils.getString(obj, FieldName.Class.FIELD_NAME);
        description = BsonUtils.getString(obj, FieldName.Class.FIELD_DESCRIPTION);
        wsName = BsonUtils.getString(obj, ScmRestArgDefine.META_DATA_WORKSPACE_NAME);
        classId = BsonUtils.getString(obj, ScmRestArgDefine.META_DATA_CLASS_ID);
        updateUser = BsonUtils.getString(obj, ScmRestArgDefine.META_DATA_UPDATE_USER);
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public String getAttachAttributeId() {
        return attachAttributeId;
    }

    public void setAttachAttributeId(String attachAttributeId) {
        this.attachAttributeId = attachAttributeId;
    }

    public String getDettachAttributeId() {
        return dettachAttributeId;
    }

    public void setDettachAttributeId(String dettachAttributeId) {
        this.dettachAttributeId = dettachAttributeId;
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

    public boolean isNeedUpdate() {
        if (attachAttributeId != null) {
            return true;
        }

        if (description != null) {
            return true;
        }

        if (dettachAttributeId != null) {
            return true;
        }

        if (name != null) {
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MetaDataClassConfigUpdator that = (MetaDataClassConfigUpdator) o;
        return Objects.equals(wsName, that.wsName) && Objects.equals(classId, that.classId)
                && Objects.equals(attachAttributeId, that.attachAttributeId)
                && Objects.equals(dettachAttributeId, that.dettachAttributeId)
                && Objects.equals(name, that.name) && Objects.equals(description, that.description)
                && Objects.equals(updateUser, that.updateUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wsName, classId, attachAttributeId, dettachAttributeId, name,
                description, updateUser);
    }
}
