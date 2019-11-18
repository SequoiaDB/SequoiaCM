package com.sequoiacm.schedule.core.meta;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.schedule.entity.SiteEntity;

public class WorkspaceInfo {
    private int id;
    private String name;
    private List<SiteEntity> siteList = new ArrayList<>();

    public WorkspaceInfo(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addSite(SiteEntity site) {
        siteList.add(site);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SiteEntity getSite(String siteName) {
        for (int i = 0; i < siteList.size(); i++) {
            SiteEntity siteEntity = siteList.get(i);
            if (siteEntity.getName().equals(siteName)) {
                return siteEntity;
            }
        }

        return null;
    }

    public List<Integer> getSiteIdList() {
        List<Integer> siteIds = new ArrayList<>();
        for (SiteEntity site : siteList) {
            siteIds.add(site.getId());
        }
        return siteIds;
    }
}
