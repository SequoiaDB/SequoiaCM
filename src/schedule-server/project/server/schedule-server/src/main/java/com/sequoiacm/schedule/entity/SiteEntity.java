package com.sequoiacm.schedule.entity;

public class SiteEntity {
    private int id;
    private String name;
    private boolean isRoot;
    private String stageTag;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }

    public String getStageTag() {
        return stageTag;
    }

    public void setStageTag(String stageTag) {
        this.stageTag = stageTag;
    }
}
