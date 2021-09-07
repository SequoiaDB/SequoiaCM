package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmScheduleBasicInfo {
    @JsonProperty("schedule_id")
    private String scheduleId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("enable")
    private boolean enable = true;

    @JsonProperty("workspace")
    private String workspace;

    @JsonProperty("description")
    private String description;

    public OmScheduleBasicInfo() {
    }

    public OmScheduleBasicInfo(String scheduleId, String name, String type, Boolean enable,
            String workspace, String description) {
        this.scheduleId = scheduleId;
        this.name = name;
        this.type = type;
        this.enable = enable;
        this.workspace = workspace;
        this.description = description;
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

    public boolean getEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
