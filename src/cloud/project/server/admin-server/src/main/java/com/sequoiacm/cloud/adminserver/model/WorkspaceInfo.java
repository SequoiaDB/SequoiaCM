package com.sequoiacm.cloud.adminserver.model;

import java.util.List;

public class WorkspaceInfo {
    private int id;
    private String name;
    private List<Integer> siteList;
    
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

    public void setSiteList(List<Integer> siteList) {
        this.siteList = siteList;
    }

    public List<Integer> getSiteList() {
        return siteList;
    }
}
