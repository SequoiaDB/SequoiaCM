package com.sequoiacm.config.framework.workspace.entity;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.FieldName;

public class RootDirEntity {
    private String id = "000000000000000000000000";
    private String name = "/";
    private String pid = "-1";
    private long createTime = 0;
    private String createUser = "";

    public RootDirEntity(String user, long time) {
        this.createTime = time;
        this.createUser = user;
    }

    public BSONObject toReocord() {
        BSONObject rec = new BasicBSONObject();
        rec.put(FieldName.FIELD_CLDIR_ID, id);
        rec.put(FieldName.FIELD_CLDIR_NAME, name);
        rec.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, pid);
        rec.put(FieldName.FIELD_CLDIR_CREATE_TIME, createTime);
        rec.put(FieldName.FIELD_CLDIR_UPDATE_TIME, createTime);
        rec.put(FieldName.FIELD_CLDIR_USER, createUser);
        rec.put(FieldName.FIELD_CLDIR_UPDATE_USER, createUser);
        return rec;
    }
}
