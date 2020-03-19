package com.sequoiacm.s3.remote;

import java.util.Set;

public class SiteInfo {
    private int id;
    private String name;
    private Set<String> region;
    private boolean isRoot;

    public SiteInfo() {
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<String> getRegion() {
        return region;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRegion(Set<String> region) {
        this.region = region;
    }

}
