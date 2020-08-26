package com.sequoiacm.fulltext.server.workspace;

import java.util.List;

import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;

public class ScmWorkspaceInfo {
    private String name;
    private int id;
    private List<Integer> sites;
    private ScmWorkspaceFulltextExtData externalData;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Integer> getSites() {
        return sites;
    }

    public void setSites(List<Integer> sites) {
        this.sites = sites;
    }

    public ScmWorkspaceFulltextExtData getExternalData() {
        return externalData;
    }

    public void setExternalData(ScmWorkspaceFulltextExtData externalData) {
        this.externalData = externalData;
    }

    @Override
    public String toString() {
        return "ScmWorkspaceInfo [name=" + name + ", id=" + id + ", sites=" + sites
                + ", externalData=" + externalData + ", getClass()=" + getClass() + ", hashCode()="
                + hashCode() + ", toString()=" + super.toString() + "]";
    }

}
