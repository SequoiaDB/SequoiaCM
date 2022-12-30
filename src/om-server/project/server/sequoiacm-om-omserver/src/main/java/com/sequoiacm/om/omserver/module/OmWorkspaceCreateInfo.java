package com.sequoiacm.om.omserver.module;

import java.util.List;
import org.bson.BSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OmWorkspaceCreateInfo {
    @JsonProperty("workspace_names")
    private List<String> wsNameList;

    @JsonProperty("cache_strategy")
    private String cacheStrategy;

    @JsonProperty("preferred")
    private String preferred;

    @JsonProperty("description")
    private String description;

    @JsonProperty("directory_enabled")
    private boolean directoryEnabled;

    @JsonProperty("meta_location")
    private BSONObject metaLocation;

    @JsonProperty("data_locations")
    private List<BSONObject> dataLocations;

    public List<String> getWsNameList() {
        return wsNameList;
    }

    public void setWsNameList(List<String> wsNameList) {
        this.wsNameList = wsNameList;
    }

    public String getCacheStrategy() {
        return cacheStrategy;
    }

    public void setCacheStrategy(String cacheStrategy) {
        this.cacheStrategy = cacheStrategy;
    }

    public String getPreferred() {
        return preferred;
    }

    public void setPreferred(String preferred) {
        this.preferred = preferred;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDirectoryEnabled() {
        return directoryEnabled;
    }

    public void setDirectoryEnabled(boolean directoryEnabled) {
        this.directoryEnabled = directoryEnabled;
    }

    public BSONObject getMetaLocation() {
        return metaLocation;
    }

    public void setMetaLocation(BSONObject metaLocation) {
        this.metaLocation = metaLocation;
    }

    public List<BSONObject> getDataLocations() {
        return dataLocations;
    }

    public void setDataLocations(List<BSONObject> dataLocations) {
        this.dataLocations = dataLocations;
    }

    @Override
    public String toString() {
        return "OmWorkspaceCreateInfo{" + "wsNameList=" + wsNameList + ", cacheStrategy='"
                + cacheStrategy + '\'' + ", preferred='" + preferred + '\'' + ", description='"
                + description + '\'' + ", directoryEnabled=" + directoryEnabled + ", metaLocation="
                + metaLocation + ", dataLocations=" + dataLocations + '}';
    }
}
