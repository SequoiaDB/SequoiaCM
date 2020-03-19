package com.sequoiacm.s3.remote;

import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;

public class ScmDirInfo {
    private String parentId;
    private String id;
    private String name;
    private String user;
    private long createTime;

    public ScmDirInfo() {
    }

    public ScmDirInfo(BSONObject parse) {
        id = BsonUtils.getStringChecked(parse, FieldName.FIELD_CLDIR_ID);
        name = BsonUtils.getStringChecked(parse, FieldName.FIELD_CLDIR_NAME);
        parentId = BsonUtils.getStringChecked(parse, FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID);
        user = BsonUtils.getStringChecked(parse, FieldName.FIELD_CLDIR_USER);
        createTime = BsonUtils.getNumberChecked(parse, FieldName.FIELD_CLDIR_CREATE_TIME)
                .longValue();
    }

    public long getCreateTime() {
        return createTime;
    }

    public String getUser() {
        return user;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public void setUser(String user) {
        this.user = user;
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

}
