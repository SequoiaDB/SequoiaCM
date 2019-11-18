package com.sequoiacm.om.omserver.module;

public class OmSiteInfo {
    private int id;
    private String name;
    private boolean isRootSite;

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

    public boolean isRootSite() {
        return isRootSite;
    }

    public void setRootSite(boolean isRootSite) {
        this.isRootSite = isRootSite;
    }

}
