package com.sequoiacm.datasource.metadata.sequoiadb;

public class SdbConfig {
    private String location;
    private String putLobRequiredVersion;

    public SdbConfig(String location) {
        this.location = location;
    }

    public SdbConfig(String location, String putLobRequiredVersion) {
        this.location = location;
        this.putLobRequiredVersion = putLobRequiredVersion;
    }

    public String getLocation() {
        return location;
    }

    public String getPutLobRequiredVersion() {
        return putLobRequiredVersion;
    }
}
