package com.sequoiacm.schedule.entity;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceEntity {
    private int id;
    private String name;
    private List<Integer> siteList = new ArrayList<>();

    public void addSite(int site) {
        siteList.add(site);
    }

    public List<Integer> getSiteList() {
        return siteList;
    }

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
}
