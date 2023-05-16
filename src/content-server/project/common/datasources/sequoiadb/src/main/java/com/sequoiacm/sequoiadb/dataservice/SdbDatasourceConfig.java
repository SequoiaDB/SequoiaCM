package com.sequoiacm.sequoiadb.dataservice;

import com.sequoiacm.datasource.metadata.sequoiadb.SdbConfig;
import com.sequoiacm.infrastructure.sdbversion.VersionRange;

import java.util.List;

public class SdbDatasourceConfig {
    private static String location = "";
    private static String putLobRequiredVersion = "3.6.1";
    private static List<VersionRange> putLobRequiredVersionRanges;

    public static String getLocation() {
        return location;
    }

    public static List<VersionRange> getPutLobRequiredVersionRanges() {
        return putLobRequiredVersionRanges;
    }

    public static void init(SdbConfig sdbConfig) {
        if (sdbConfig == null) {
            putLobRequiredVersionRanges = VersionRange.parse(putLobRequiredVersion);
            return;
        }
        String locationConf = sdbConfig.getLocation();
        if (locationConf != null) {
            location = locationConf;
        }

        String putLobRequiredVersionConf = sdbConfig.getPutLobRequiredVersion();
        if (putLobRequiredVersionConf != null) {
            putLobRequiredVersion = putLobRequiredVersionConf;
        }
        putLobRequiredVersionRanges = VersionRange.parse(putLobRequiredVersion);
    }
}
