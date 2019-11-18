package com.sequoiacm.schedule.core.meta;

import java.util.List;

import com.sequoiacm.schedule.core.ScheduleServer;
import com.sequoiacm.schedule.entity.FileServerEntity;

public class SiteInfo {
    private int id;
    private String name;
    private boolean isRoot;

    public SiteInfo(int id, String name, boolean isRoot) {
        this.id = id;
        this.name = name;
        this.isRoot = isRoot;
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
