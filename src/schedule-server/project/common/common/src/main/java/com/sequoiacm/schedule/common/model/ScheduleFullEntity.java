package com.sequoiacm.schedule.common.model;

import org.bson.BSONObject;

import com.sequoiacm.schedule.common.FieldName;

public class ScheduleFullEntity {
    private ScheduleUserEntity userInfo = new ScheduleUserEntity();
    private String id;
    private String create_user;
    private long create_time;

    public ScheduleFullEntity() {
    }

    public ScheduleFullEntity(String scheduleId, String name, String desc, String type,
            String workspace, BSONObject content, String cron, boolean enable, String createUser,
            long createTime) {
        this.id = scheduleId;
        setName(name);
        setDesc(desc);
        setType(type);
        setWorkspace(workspace);
        setContent(content);
        setCron(cron);
        setEnable(enable);
        this.create_user = createUser;
        this.create_time = createTime;
    }

    public void setEnable(boolean enable) {
        this.userInfo.setEnable(enable);
    }

    public boolean isEnable() {
        return this.userInfo.isEnable();
    }

    public String getName() {
        return this.userInfo.getName();
    }

    public void setName(String name) {
        this.userInfo.setName(name);
    }

    public String getDesc() {
        return this.userInfo.getDesc();
    }

    public void setDesc(String desc) {
        this.userInfo.setDesc(desc);
    }

    public String getType() {
        return this.userInfo.getType();
    }

    public void setType(String type) {
        this.userInfo.setType(type);
    }

    public String getWorkspace() {
        return this.userInfo.getWorkspace();
    }

    public void setWorkspace(String workspace) {
        this.userInfo.setWorkspace(workspace);
    }

    public BSONObject getContent() {
        return this.userInfo.getContent();
    }

    public void setContent(BSONObject content) {
        this.userInfo.setContent(content);
    }

    public String getCron() {
        return this.userInfo.getCron();
    }

    public void setCron(String cron) {
        this.userInfo.setCron(cron);
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
                .append(",").append(FieldName.Schedule.FIELD_ENABLE).append(":").append(isEnable());

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
        return newInfo;

    }
}
