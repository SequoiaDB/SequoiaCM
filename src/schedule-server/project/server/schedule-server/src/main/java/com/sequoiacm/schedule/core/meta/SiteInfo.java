package com.sequoiacm.schedule.core.meta;

import java.util.List;

import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.entity.FileServerEntity;

public class SiteInfo {
    private int id;
    private String name;
    private boolean isRoot;
    private String stageTag;

    public SiteInfo(int id, String name, boolean isRoot,String stageTag) {
        this.id = id;
        this.name = name;
        this.isRoot = isRoot;
        this.stageTag = stageTag;
    }

    public String getStageTag(){
        return stageTag;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public List<FileServerEntity> getServers() {
        return ScheduleServer.getInstance().getServersBySiteId(id);
    }
}
