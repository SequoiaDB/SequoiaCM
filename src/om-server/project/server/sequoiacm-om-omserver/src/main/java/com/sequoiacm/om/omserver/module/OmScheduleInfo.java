package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.BSONObject;

import javax.validation.constraints.NotNull;
import java.util.Date;

public class OmScheduleInfo {

    // 分组校验-创建调度任务
    public interface CREATE{}

    // 分组校验-更新调度任务
    public interface UPDATE{}

    @JsonProperty("schedule_id")
    private String scheduleId;

    @NotNull(message = "schedule name is required", groups = CREATE.class)
    @JsonProperty("name")
    private String name;

    @NotNull(message = " schedule type is required", groups = CREATE.class)
    @JsonProperty("type")
    private String type;

    @JsonProperty("enable")
    private Boolean enable;

    @NotNull(message = "workspace is required", groups = CREATE.class)
    @JsonProperty("workspace")
    private String workspace;

    @NotNull(message = "cron is required", groups = CREATE.class)
    @JsonProperty("cron")
    private String cron;

    @JsonProperty("description")
    private String description;

    @JsonProperty("create_user")
    private String createUser;

    @JsonProperty("create_time")
    private Date createTime;

    @JsonProperty("preferred_region")
    private String preferredRegion;

    @JsonProperty("preferred_zone")
    private String preferredZone;

    @JsonProperty("content")
    private BSONObject content;

    public OmScheduleInfo() {
    }

    public OmScheduleInfo(String scheduleId, String name, String type, Boolean enable,
            String workspace, String cron, String description, String createUser, Date createTime,
            String preferredRegion, String preferredZone, BSONObject content) {
        this.scheduleId = scheduleId;
        this.name = name;
        this.type = type;
        this.enable = enable;
        this.workspace = workspace;
        this.cron = cron;
        this.description = description;
        this.createUser = createUser;
        this.createTime = createTime;
        this.preferredRegion = preferredRegion;
        this.preferredZone = preferredZone;
        this.content = content;
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
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

    public BSONObject getContent() {
        return content;
    }

    public void setContent(BSONObject content) {
        this.content = content;
    }
}
