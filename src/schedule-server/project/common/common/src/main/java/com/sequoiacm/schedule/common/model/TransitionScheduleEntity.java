package com.sequoiacm.schedule.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.schedule.common.FieldName;
import org.bson.types.BasicBSONList;

public class TransitionScheduleEntity {
    private String id;
    private String workspace;
    private TransitionUserEntity transition;
    private boolean customized;
    @JsonProperty("global_transition_id")
    private String globalTransitionId;
    @JsonProperty("schedule_ids")
    private BasicBSONList scheduleIds;
    @JsonProperty("create_time")
    private long createTime;
    @JsonProperty("create_user")
    private String createUser;
    private boolean enable;
    @JsonProperty("preferred_region")
    private String preferredRegion;
    @JsonProperty("preferred_zone")
    private String preferredZone;
    @JsonProperty("update_user")
    private String updateUser;
    @JsonProperty("update_time")
    private long updateTime;

    public TransitionScheduleEntity() {
    }

    public TransitionScheduleEntity(String id, String workspace, TransitionUserEntity transition,
                                    boolean customized, long createTime, String createUser, boolean enable,
                                    String preferredRegion, String preferredZone, String updateUser, long updateTime) {
        this.id = id;
        this.workspace = workspace;
        this.transition = transition;
        this.customized = customized;
        this.createTime = createTime;
        this.createUser = createUser;
        this.enable = enable;
        this.preferredRegion = preferredRegion;
        this.preferredZone = preferredZone;
        this.updateTime = updateTime;
        this.updateUser = updateUser;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public TransitionUserEntity getTransition() {
        return transition;
    }

    public void setTransition(TransitionUserEntity transition) {
        this.transition = transition;
    }

    public boolean getCustomized() {
        return customized;
    }

    public void setCustomized(boolean customized) {
        this.customized = customized;
    }

    public BasicBSONList getScheduleIds() {
        return scheduleIds;
    }

    public void setScheduleIds(BasicBSONList scheduleIds) {
        this.scheduleIds = scheduleIds;
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

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getPreferredRegion() {
        return preferredRegion;
    }

    public void setPreferredRegion(String preferredRegion) {
        this.preferredRegion = preferredRegion;
    }

    public String getPreferredZone() {
        return preferredZone;
    }

    public void setPreferredZone(String preferredZone) {
        this.preferredZone = preferredZone;
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

    public String getGlobalTransitionId() {
        return globalTransitionId;
    }

    public void setGlobalTransitionId(String globalTransitionId) {
        this.globalTransitionId = globalTransitionId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ID).append(":")
                .append(getId()).append(",").append(FieldName.LifeCycleConfig.FIELD_WORKSPACE_NAME)
                .append(":").append(getWorkspace()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_TRANSITION)
                .append(":").append(getTransition()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_HADCUSTOM)
                .append(":").append(getCustomized()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_IDS).append(":")
                .append(getScheduleIds()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_CREATE_TIME)
                .append(":").append(getCreateTime()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_CREATE_USER)
                .append(":").append(getCreateUser()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_UPDATE_TIME)
                .append(":").append(getUpdateTime()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_UPDATE_USER)
                .append(":").append(getUpdateUser()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_LIFE_CYCLE_CONFIG_SCHEDULE_ENABLE)
                .append(":").append(isEnable()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_PREFERRED_REGION).append(":")
                .append(getPreferredRegion()).append(",")
                .append(FieldName.LifeCycleConfig.FIELD_PREFERRED_ZONE).append(":")
                .append(getPreferredZone());
        return sb.toString();
    }
}
