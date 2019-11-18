package com.sequoiacm.deploy.module;

public enum InstallPackType {
    CLOUD("^sequoiacm-cloud-([0-9\\.]*)-release\\.tar\\.gz$", "sequoiacm-cloud"),
    CONFIG_SERVER("^sequoiacm-config-([0-9\\\\.]*)-release\\.tar\\.gz$", "sequoiacm-config"),
    SCHEDULE_SERVER("^sequoiacm-schedule-([0-9\\\\.]*)-release\\.tar\\.gz$", "sequoiacm-schedule"),
    CONTENTSERVER("^contentserver-([0-9\\\\.]*)-release\\.tar\\.gz$", "contentserver"),
    OM_SERVER("^sequoiacm-om-([0-9\\\\.]*)-release\\.tar\\.gz$", "sequoiacm-om"),
    CLOUD_DISK("^sequoiacm-cloud-disk-([0-9\\\\.]*)-release\\.tar\\.gz$", "sequoiacm-cloud-disk"),
    VIRTUAL_CLOUD_DISK("^cloud-disk-([0-9\\\\.]*)-release\\.tar\\.gz$", "cloud-disk"),

    ZOOKEEPER("^zookeeper-(.*)\\.tar\\.gz$") {
        @Override
        public String getUntarDirName(String packName) {
            return packName.replace(".tar.gz", "");
        }
    };

    private String packNameRegexp;
    private String untarDirName;

    private InstallPackType(String packName, String untarDirName) {
        this.packNameRegexp = packName;
        this.untarDirName = untarDirName;
    }

    private InstallPackType(String packName) {
        this.packNameRegexp = packName;
    }

    public String getPackNameRegexp() {
        return packNameRegexp;
    }

    public String getUntarDirName(String packName) {
        return untarDirName;
    }

}
