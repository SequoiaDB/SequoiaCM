package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmWorkspaceInfo {

    @JsonProperty("site_cache_strategy")
    private String siteCacheStrategy;

    @JsonProperty("tag_retrieval_enabled")
    private Boolean tagRetrievalEnabled;

    public Boolean isTagRetrievalEnabled() {
        return tagRetrievalEnabled;
    }

    public void setTagRetrievalEnabled(Boolean tagRetrievalEnabled) {
        this.tagRetrievalEnabled = tagRetrievalEnabled;
    }

    public String getSiteCacheStrategy() {
        return siteCacheStrategy;
    }

    public void setSiteCacheStrategy(String siteCacheStrategy) {
        this.siteCacheStrategy = siteCacheStrategy;
    }
}
