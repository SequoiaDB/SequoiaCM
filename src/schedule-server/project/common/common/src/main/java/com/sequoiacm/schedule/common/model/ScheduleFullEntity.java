package com.sequoiacm.schedule.common.model;

import org.bson.BSONObject;

import com.sequoiacm.schedule.common.FieldName;

public class ScheduleFullEntity extends ScheduleUserEntity {
    private String id;
    private String create_user;
    private long create_time;

    public ScheduleFullEntity() {
    }

    public ScheduleFullEntity(String scheduleId, String name, String desc, String type,
            String workspace, BSONObject content, String cron, boolean enable, String createUser,
            long createTime, String preferredRegion, String preferredZone) {
        super(name, desc, type, workspace, content, cron, enable, preferredRegion, preferredZone);
        this.id = scheduleId;
        this.create_user = createUser;
        this.create_time = createTime;
    }

    public String getCreate_user() {
        return create_user;
    }

    public void setCreate_user(String createUser) {
        this.create_user = createUser;
    }

    public long getCreate_time() {
        return create_time;
    }

    public void setCreate_time(long createTime) {
        this.create_time = createTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FieldName.Schedule.FIELD_ID).append(":").append(getId()).append(",")
                .append(FieldName.Schedule.FIELD_NAME).append(":").append(getName()).append(",")
                .append(FieldName.Schedule.FIELD_DESC).append(":").append(getDesc()).append(",")
                .append(FieldName.Schedule.FIELD_TYPE).append(":").append(getType()).append(",")
                .append(FieldName.Schedule.FIELD_WORKSPACE).append(":").append(getWorkspace())
                .append(",").append(FieldName.Schedule.FIELD_CONTENT).append(":")
                .append(getContent()).append(",").append(FieldName.Schedule.FIELD_CRON).append(":")
                .append(getCron()).append(",").append(FieldName.Schedule.FIELD_CREATE_USER)
                .append(":").append(getCreate_user()).append(",")
                .append(FieldName.Schedule.FIELD_CREATE_TIME).append(":").append(getCreate_time())
                .append(",").append(FieldName.Schedule.FIELD_ENABLE).append(":").append(isEnable())
                .append(",").append(FieldName.Schedule.FIELD_PREFERRED_REGION).append(":")
                .append(getPreferredRegion()).append(",")
                .append(FieldName.Schedule.FIELD_PREFERRED_ZONE).append(":")
                .append(getPreferredZone());
        return sb.toString();
    }

    @Override
    public ScheduleFullEntity clone() {
        ScheduleFullEntity newInfo = new ScheduleFullEntity();
        newInfo.setContent(this.getContent());
        newInfo.setCreate_time(this.getCreate_time());
        newInfo.setCreate_user(this.getCreate_user());
        newInfo.setCron(this.getCron());
        newInfo.setDesc(this.getDesc());
        newInfo.setId(this.getId());
        newInfo.setName(this.getName());
        newInfo.setType(this.getType());
        newInfo.setWorkspace(this.getWorkspace());
        newInfo.setEnable(this.isEnable());
        newInfo.setPreferredRegion(this.preferredRegion);
        newInfo.setPreferredZone(this.preferredZone);
        return newInfo;

    }
}
