package com.sequoiacm.infrastructure.config.core.msg.metadata;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;

public class MetaDataClassConfig {
    private String wsName;
    private String id;
    private String name;
    private String description = "";
    private String createUser;
    private long createTime;
    private String updateUser;
    private long updateTime;

    public MetaDataClassConfig(BSONObject object) {
        wsName = BsonUtils.getString(object, ScmRestArgDefine.META_DATA_WORKSPACE_NAME);
        id = BsonUtils.getString(object, FieldName.ClassTable.FIELD_ID);
        name = BsonUtils.getString(object, FieldName.ClassTable.FIELD_NAME);
        description = BsonUtils.getStringOrElse(object, FieldName.ClassTable.FIELD_DESCRIPTION, "");
        createUser = BsonUtils.getString(object, FieldName.ClassTable.FIELD_INNER_CREATE_USER);
        createTime = BsonUtils
                .getNumberOrElse(object, FieldName.ClassTable.FIELD_INNER_CREATE_TIME, 0)
                .longValue();
        updateUser = BsonUtils.getString(object, FieldName.ClassTable.FIELD_INNER_UPDATE_USER);
        updateTime = BsonUtils
                .getNumberOrElse(object, FieldName.ClassTable.FIELD_INNER_UPDATE_TIME, 0)
                .longValue();
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
        record.put(FieldName.ClassTable.FIELD_DESCRIPTION, description);
        record.put(FieldName.ClassTable.FIELD_ID, id);
        record.put(FieldName.ClassTable.FIELD_INNER_CREATE_TIME, createTime);
        record.put(FieldName.ClassTable.FIELD_INNER_CREATE_USER, createUser);
        record.put(FieldName.ClassTable.FIELD_INNER_UPDATE_TIME, updateTime);
        record.put(FieldName.ClassTable.FIELD_INNER_UPDATE_USER, updateUser);
        record.put(FieldName.ClassTable.FIELD_NAME, name);
        return record;
    }

    public BSONObject toBSONObject() {
        BSONObject obj = toRecord();
        obj.put(ScmRestArgDefine.META_DATA_WORKSPACE_NAME, wsName);
        return obj;
    }
}
