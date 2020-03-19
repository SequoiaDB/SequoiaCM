package com.sequoiacm.deploy.module;

public enum InstallPackType {
    CLOUD("^sequoiacm-cloud-((?!disk).)*-release\\.tar\\.gz$", "sequoiacm-cloud"),
    CONFIG_SERVER("^sequoiacm-config-(.*)-release\\.tar\\.gz$", "sequoiacm-config"),
    SCHEDULE_SERVER("^sequoiacm-schedule-(.*)-release\\.tar\\.gz$", "sequoiacm-schedule"),
    CONTENTSERVER("^sequoiacm-content-(.*)-release\\.tar\\.gz$", "sequoiacm-content"),
    OM_SERVER("^sequoiacm-om-(.*)-release\\.tar\\.gz$", "sequoiacm-om"),
    CLOUD_DISK("^sequoiacm-cloud-disk-(.*)-release\\.tar\\.gz$", "sequoiacm-cloud-disk"),
    VIRTUAL_CLOUD_DISK("^cloud-disk-(.*)-release\\.tar\\.gz$", "cloud-disk"),
    MQ_SERVER("^sequoiacm-mq-(.*)-release\\.tar\\.gz$", "sequoiacm-mq"),
    S3_SERVER("^sequoiacm-s3-(.*)-release\\.tar\\.gz$", "sequoiacm-s3"),

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
