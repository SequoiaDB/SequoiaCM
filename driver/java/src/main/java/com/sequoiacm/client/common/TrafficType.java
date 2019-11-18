package com.sequoiacm.client.common;

import com.sequoiacm.common.CommonDefine;

/**
 * Traffic type.
 */
public enum TrafficType {
    /**
     * Upload file.
     */
    FILE_UPLOAD(CommonDefine.TrafficType.SCM_TRAFFIC_TYPE_FILEUPLOAD),

    /**
     * Download file.
     */
    FILE_DOWNLOAD(CommonDefine.TrafficType.SCM_TRAFFIC_TYPE_FILEDOWNLOAD),

    /**
     * Unknown type.
     */
    UNKOWN("unknown");

    private String name;

    private TrafficType(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the type.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the type with specified name.
     *
     * @param name
     *            type name.
     * @return TrafficType.
     */
    public static TrafficType getType(String name) {
        for (TrafficType type : TrafficType.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }

        return UNKOWN;
    }
}
