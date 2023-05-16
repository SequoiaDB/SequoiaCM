package com.sequoiacm.contentserver.config;

import com.sequoiacm.infrastructure.sdbversion.VersionRange;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.datasource.sequoiadb")
public class SdbDatasourceConfig {
    private String location = "";
    private String putLobRequiredVersion = "3.6.1";

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPutLobRequiredVersion() {
        return putLobRequiredVersion;
    }

    public void setPutLobRequiredVersion(String putLobRequiredVersion) {
        VersionRange.parse(putLobRequiredVersion);
        this.putLobRequiredVersion = putLobRequiredVersion;
    }
}
