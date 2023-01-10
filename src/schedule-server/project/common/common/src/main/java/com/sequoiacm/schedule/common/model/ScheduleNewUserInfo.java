package com.sequoiacm.schedule.common.model;

import org.bson.BSONObject;

public class ScheduleNewUserInfo {
    protected String name;
    protected String desc;
    protected String type;
    protected String workspace;
    protected BSONObject content;
    protected String cron;
    protected Boolean enable;
    protected String preferredRegion;
    protected String preferredZone;
    protected String transition;

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

    public Boolean isEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getPreferredZone() {
        return preferredZone;
    }

    public void setPreferredZone(String preferredZone) {
        this.preferredZone = preferredZone;
    }

    public String getPreferredRegion() {
        return preferredRegion;
    }

    public void setPreferredRegion(String preferredRegion) {
        this.preferredRegion = preferredRegion;
    }

    public String getTransition() {
        return transition;
    }

    public void setTransition(String transition) {
        this.transition = transition;
    }
}
