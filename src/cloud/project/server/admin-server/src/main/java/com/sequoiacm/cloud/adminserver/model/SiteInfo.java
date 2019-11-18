package com.sequoiacm.cloud.adminserver.model;

import java.util.ArrayList;
import java.util.List;

public class SiteInfo {
    private int id;
    private String name;
    private boolean isRoot;
    private List<ContentServerInfo> contentServerList = new ArrayList<>();

    public SiteInfo() {
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    public boolean isRoot() {
        return isRoot;
    }
    
    public void setRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }

    public void addServer(ContentServerInfo contentServer) {
        contentServerList.add(contentServer);
    }
    
    public void setServers(List<ContentServerInfo> contentServerList) {
        this.contentServerList = contentServerList;
    }

    public List<ContentServerInfo> getServers() {
        return contentServerList;
    }
}
