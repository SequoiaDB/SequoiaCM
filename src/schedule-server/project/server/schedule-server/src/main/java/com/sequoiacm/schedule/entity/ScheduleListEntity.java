package com.sequoiacm.schedule.entity;

public class ScheduleListEntity {
    private String id;
    private String name;
    private String desc;
    private String type;
    private String workspace;
    private boolean enable;

    public ScheduleListEntity(String id, String name, String desc, String type, String workspace,
            boolean enable) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.type = type;
        this.workspace = workspace;
        this.enable = enable;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getType() {
        return type;
    }

    public String getWorkspace() {
        return workspace;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
