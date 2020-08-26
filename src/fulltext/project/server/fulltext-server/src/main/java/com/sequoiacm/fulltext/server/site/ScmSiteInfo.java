package com.sequoiacm.fulltext.server.site;

public class ScmSiteInfo {
    private boolean isRoot;
    private String name;
    private int siteId;
    public ScmSiteInfo() {
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

}
