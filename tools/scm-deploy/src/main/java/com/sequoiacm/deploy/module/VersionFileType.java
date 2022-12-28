package com.sequoiacm.deploy.module;

public enum VersionFileType {
    JAR(".jar"),
    TAR(".tar.gz"),
    PDF(".pdf");

    private String suffix;

    private VersionFileType(String suffix) {
        this.suffix = suffix;
    }

    public static String getVersion(String fileName) {
        VersionFileType type = getType(fileName);
        if (type == null) {
            return null;
        }
        String version = null;
        int versionStart = 0;
        int versionEnd = 0;
        switch (type) {
            case JAR:
                versionStart = fileName.lastIndexOf("-");
                versionEnd = fileName.lastIndexOf(type.getSuffix());
                break;
            case TAR:
                if (fileName.contains("release")) {
                    versionEnd = fileName.lastIndexOf("-release" + type.getSuffix());
                    versionStart = fileName.lastIndexOf("-", versionEnd - 1);
                } else {
                    versionStart = fileName.lastIndexOf("-");
                    versionEnd = fileName.lastIndexOf(type.getSuffix());
                }
                break;
            case PDF:
                versionStart = fileName.lastIndexOf("v");
                versionEnd = fileName.lastIndexOf(type.getSuffix());
        }
        version = fileName.substring(versionStart + 1, versionEnd);
        return version;
    }

    public static VersionFileType getType(String fileName) {
        for (VersionFileType value : VersionFileType.values()) {
            if (fileName.endsWith(value.suffix)) {
                return value;
            }
        }
        return null;
    }

    public String getSuffix() {
        return suffix;
    }

    public static int compareVersion(String newVersion, String oldVersion) {
        if (newVersion == null && oldVersion==null) {
            return 0;
        }
        if (newVersion == null) {
            return -1;
        }
        if (oldVersion == null) {
            return  1;
        }
        String[] newVersionArray = newVersion.split("\\.");
        String[] oldVersionArray = oldVersion.split("\\.");
        for (int i = 0; i < 3; i++) {
            String newVersionNumber = null;
            String oldVersionNumber = null;
            if (i < newVersionArray.length) {
                newVersionNumber = newVersionArray[i];
            }
            if (i < oldVersionArray.length) {
                oldVersionNumber = oldVersionArray[i];
            }
            if (newVersionNumber == null) {
                if (oldVersionNumber == null) {
                    return 0;
                }
                return -1;
            }
            if (oldVersionNumber == null) {
                return 1;
            }
            int compare = Integer.valueOf(newVersionNumber).compareTo(Integer.valueOf(oldVersionNumber));
            if (compare == 0) {
                continue;
            }
            return compare;
        }
        return 0;
    }
}