package com.sequoiacm.schedule.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.BSONObject;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.FieldName.Schedule;

public class ScheduleUserEntity {
    protected String name;
    protected String desc;
    protected String type;
    protected String workspace;
    protected BSONObject content;
    protected String cron;
    protected boolean enable = true;
    @JsonProperty(Schedule.FIELD_PREFERRED_REGION)
    protected String preferredRegion;
    @JsonProperty(Schedule.FIELD_PREFERRED_ZONE)
    protected String preferredZone;
    protected String transitionId;

    public ScheduleUserEntity() {
    }

    public ScheduleUserEntity(String name, String desc, String type, String workspace,
            BSONObject content, String cron, boolean enable, String preferredRegion,
            String preferredZone, String transitionId) {
        this.name = name;
        this.desc = desc;
        this.type = type;
        this.workspace = workspace;
        this.content = content;
        this.cron = cron;
        this.enable = enable;
        this.preferredRegion = preferredRegion;
        this.preferredZone = preferredZone;
        this.transitionId = transitionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public BSONObject getContent() {
        return content;
    }

    public void setContent(BSONObject content) {
        this.content = content;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
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

    public String getTransitionId() {
        return transitionId;
    }

    public void setTransitionId(String transitionId) {
        this.transitionId = transitionId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FieldName.Schedule.FIELD_NAME).append(":").append(name).append(",")
                .append(FieldName.Schedule.FIELD_DESC).append(":").append(desc).append(",")
                .append(FieldName.Schedule.FIELD_TYPE).append(":").append(type).append(",")
                .append(FieldName.Schedule.FIELD_WORKSPACE).append(":").append(workspace)
                .append(",").append(FieldName.Schedule.FIELD_CONTENT).append(":").append(content)
                .append(",").append(FieldName.Schedule.FIELD_CRON).append(":").append(cron)
                .append(",").append(FieldName.Schedule.FIELD_ENABLE).append(":").append(enable)
                .append(",").append(Schedule.FIELD_PREFERRED_REGION).append(":").append(preferredRegion)
                .append(",").append(Schedule.FIELD_PREFERRED_ZONE).append(":").append(preferredZone);

        return sb.toString();
    }
}
