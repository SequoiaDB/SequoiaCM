package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmWorkspaceInfo {

    @JsonProperty("site_cache_strategy")
    private String siteCacheStrategy;

    public String getSiteCacheStrategy() {
        return siteCacheStrategy;
    }

    public void setSiteCacheStrategy(String siteCacheStrategy) {
        this.siteCacheStrategy = siteCacheStrategy;
    }
}
