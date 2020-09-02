package com.sequoiacm.infrastructure.config.core.msg.workspace;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.infrastructure.config.core.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.Config;

public class WorkspaceConfig implements Config {
    private String wsName;
    private int wsId;
    private String createUser;
    private String updateUser;
    private String desc = "";
    private Long createTime;
    private Long updateTime;
    private BSONObject metalocation;
    private BasicBSONList dataLocations;
    private BSONObject externalData;

    public WorkspaceConfig() {
    }

    public BSONObject getExternalData() {
        return externalData;
    }

    public void setExternalData(BSONObject externalData) {
        this.externalData = externalData;
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public BSONObject getMetalocation() {
        return metalocation;
    }

    public void setMetalocation(BSONObject metalocation) {
        this.metalocation = metalocation;
    }

    public BasicBSONList getDataLocations() {
        return dataLocations;
    }

    public void setDataLocations(BasicBSONList dataLocations) {
        this.dataLocations = dataLocations;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public int getWsId() {
        return wsId;
    }

    public void setWsId(int wsId) {
        this.wsId = wsId;
    }

    @Override
    public BSONObject toBSONObject() {
        BSONObject wsConfigObj = new BasicBSONObject();
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_NAME, wsName);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_ID, wsId);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_DESCRIPTION, desc);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_CREATEUSER, createUser);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_UPDATEUSER, updateUser);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_CREATETIME, createTime);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_UPDATETIME, updateTime);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_META_LOCATION, metalocation);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION, dataLocations);
        wsConfigObj.put(FieldName.FIELD_CLWORKSPACE_EXTERNAL_DATA, externalData);
        return wsConfigObj;
    }
}
