package com.sequoiacm.infrastructure.config.core.msg.bucket;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.Config;

public class BucketConfig implements Config {
    private String name;
    private long id;
    private String fileTable;
    private long createTime;
    private String createUser;
    private String workspace;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFileTable() {
        return fileTable;
    }

    public void setFileTable(String fileTable) {
        this.fileTable = fileTable;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    @Override
    public BSONObject toBSONObject() {
        BasicBSONObject ret = new BasicBSONObject();
        ret.put(FieldName.Bucket.NAME, name);
        ret.put(FieldName.Bucket.ID, id);
        ret.put(FieldName.Bucket.WORKSPACE, workspace);
        ret.put(FieldName.Bucket.CREATE_TIME, createTime);
        ret.put(FieldName.Bucket.CREATE_USER, createUser);
        ret.put(FieldName.Bucket.FILE_TABLE, fileTable);
        return ret;
    }

    @Override
    public String toString() {
        return "BucketConfig{" + "name='" + name + '\'' + ", id='" + id + '\'' + ", fileTable='"
                + fileTable + '\'' + ", createTime=" + createTime + ", createUser='" + createUser
                + '\'' + ", workspace='" + workspace + '\'' + '}';
    }
}
