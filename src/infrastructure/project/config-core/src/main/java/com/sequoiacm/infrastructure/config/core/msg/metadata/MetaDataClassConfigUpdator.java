package com.sequoiacm.infrastructure.config.core.msg.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class MetaDataClassConfigUpdator {
    // update target
    private String wsName;
    private String classId;

    // new property
    private String attachAttributeId;
    private String dettachAttributeId;
    private String name;
    private String description;
    private String updateUser;

    public MetaDataClassConfigUpdator(String wsName, String classId, String updateUser) {
        this.updateUser = updateUser;
        this.wsName = wsName;
        this.classId = classId;
    }

    public MetaDataClassConfigUpdator(BSONObject obj) {
        attachAttributeId = BsonUtils.getString(obj,
                ScmRestArgDefine.META_DATA_ATTACH_ATTRUBUTE_ID);
        dettachAttributeId = BsonUtils.getString(obj,
                ScmRestArgDefine.META_DATA_DETTACH_ATTRUBUTE_ID);
        name = BsonUtils.getString(obj, FieldName.ClassTable.FIELD_NAME);
        description = BsonUtils.getString(obj, FieldName.ClassTable.FIELD_DESCRIPTION);
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

    public BSONObject toBSONObject() {
        BasicBSONObject obj = new BasicBSONObject();
        obj.put(FieldName.ClassTable.FIELD_DESCRIPTION, description);
        obj.put(FieldName.ClassTable.FIELD_NAME, name);
        obj.put(ScmRestArgDefine.META_DATA_ATTACH_ATTRUBUTE_ID, attachAttributeId);
        obj.put(ScmRestArgDefine.META_DATA_DETTACH_ATTRUBUTE_ID, dettachAttributeId);
        obj.put(ScmRestArgDefine.META_DATA_WORKSPACE_NAME, wsName);
        obj.put(ScmRestArgDefine.META_DATA_CLASS_ID, classId);
        obj.put(ScmRestArgDefine.META_DATA_UPDATE_USER, updateUser);
        return obj;
    }

}
