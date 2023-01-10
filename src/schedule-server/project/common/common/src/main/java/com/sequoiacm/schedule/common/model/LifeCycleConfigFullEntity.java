package com.sequoiacm.schedule.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.schedule.common.FieldName;
import org.bson.types.BasicBSONList;

public class LifeCycleConfigFullEntity extends LifeCycleConfigUserEntity{
    @JsonProperty("create_user")
    private String createUser;
    @JsonProperty("create_time")
    private long createTime;
    @JsonProperty("update_time")
    private long updateTime;
    @JsonProperty("update_user")
    private String updateUser;

    public LifeCycleConfigFullEntity() {

    }

    public LifeCycleConfigFullEntity(BasicBSONList stageTagConfig, BasicBSONList transitionConfig, String createUser, long createTime, String updateUser, long updateTime) {
        super(stageTagConfig, transitionConfig);
        this.createUser = createUser;
        this.createTime = createTime;
        this.updateUser = updateUser;
        this.updateTime = updateTime;
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

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_CONFIG).append(":")
                .append(getStageTagConfig()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_TRANSITION_CONFIG).append(":")
                .append(getTransitionConfig()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_CREATE_USER).append(":")
                .append(getCreateUser()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_CREATE_TIME).append(":")
                .append(getCreateTime()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_UPDATE_USER).append(":")
                .append(getUpdateUser()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_UPDATE_USER).append(":")
                .append(getUpdateUser()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_UPDATE_TIME).append(":")
                .append(getUpdateTime());
        return sb.toString();
    }
}
