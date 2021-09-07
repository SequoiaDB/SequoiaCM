package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OmSiteInfo {
    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("is_root_site")
    private boolean isRootSite;

    @JsonIgnore
    private List<String> dataUrl;

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

    public List<String> getDataUrl() {
        return dataUrl;
    }

    public void setDataUrl(List<String> dataUrl) {
        this.dataUrl = dataUrl;
    }
}
