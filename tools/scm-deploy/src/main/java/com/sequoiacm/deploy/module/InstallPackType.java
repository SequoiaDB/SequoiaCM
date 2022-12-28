package com.sequoiacm.deploy.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum InstallPackType {

    CLOUD("^sequoiacm-cloud-((?!disk).)*-release\\.tar\\.gz$", "sequoiacm-cloud"),
    CONFIG_SERVER("^sequoiacm-config-(.*)-release\\.tar\\.gz$", "sequoiacm-config"),
    SCHEDULE_SERVER("^sequoiacm-schedule-(.*)-release\\.tar\\.gz$", "sequoiacm-schedule"),
    CONTENTSERVER("^sequoiacm-content-(.*)-release\\.tar\\.gz$", "sequoiacm-content"),
    OM_SERVER("^sequoiacm-om-(.*)-release\\.tar\\.gz$", "sequoiacm-om"),
    MQ_SERVER("^sequoiacm-mq-(.*)-release\\.tar\\.gz$", "sequoiacm-mq"),
    FULLTEXT_SERVER("^sequoiacm-fulltext-(.*)-release\\.tar\\.gz$", "sequoiacm-fulltext"),
    S3_SERVER("^sequoiacm-s3-(.*)-release\\.tar\\.gz$", "sequoiacm-s3"),
    ZOOKEEPER("^zookeeper-(.*)\\.tar\\.gz$") {
        @Override
        public String getUntarDirName(String packName) {
            return packName.replace(".tar.gz", "");
        }

        @Override
        public String getUntarDirName() {
            return "zookeeper3.4.12";
        }
    },
    DAEMON("^daemon-(.*)-release\\.tar\\.gz$", "daemon"),
    NON_SERVICE("sequoiacm-driver-(.*).tar.gz", "driver") {
        @Override
        public List<String> getDirs() {
            return Arrays.asList("doc", "driver", "tools");
        }

        @Override
        public String toString() {
            return super.toString() + getDirs();
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

    public String getUntarDirName() {
        return untarDirName;
    }
    public static InstallPackType getType(String untarDirName) {
        for (InstallPackType value : InstallPackType.values()) {
            if (value.getUntarDirName().equals(untarDirName)) {
                return value;
            }
        }
        return null;
    }

    public List<String> getDirs() {
        return Collections.singletonList(untarDirName);
    }
}
