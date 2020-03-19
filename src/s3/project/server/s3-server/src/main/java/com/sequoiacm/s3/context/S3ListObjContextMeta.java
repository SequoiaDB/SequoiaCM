package com.sequoiacm.s3.context;

import com.sequoiacm.s3.common.S3CommonDefine;

public class S3ListObjContextMeta {
    private String prefix;
    private String startAfter;
    private String delimiter;
    private String lastMarker;
    private String bucketName;
    private long updateTime;
    private String ws;
    private String bucketDir;
    private String id;

    public S3ListObjContextMeta() {
    }

    public S3ListObjContextMeta(String id, String bucketName, String ws, String bucketDir,
            String prefix, String startAfter, String delimiter, String lastMarker) {
        this.id = id;
        this.ws = ws;
        this.bucketName = bucketName;
        this.bucketDir = bucketDir;
        if (prefix != null && !prefix.startsWith(S3CommonDefine.SCM_DIR_SEP)) {
            prefix = S3CommonDefine.SCM_DIR_SEP + prefix;
        }
        this.prefix = prefix;
        if (startAfter != null && !startAfter.startsWith(S3CommonDefine.SCM_DIR_SEP)) {
            startAfter = S3CommonDefine.SCM_DIR_SEP + startAfter;
        }
        this.startAfter = startAfter;
        this.delimiter = delimiter;
        if (lastMarker != null && !lastMarker.startsWith(S3CommonDefine.SCM_DIR_SEP)) {
            lastMarker = S3CommonDefine.SCM_DIR_SEP + lastMarker;
        }
        this.lastMarker = lastMarker;
        this.updateTime = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getBucketDir() {
        return bucketDir;
    }

    public String getWs() {
        return ws;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public String getLastMarker() {
        return lastMarker;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getStartAfter() {
        return startAfter;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void updateLastMarker(String lastMaker) {
        this.lastMarker = lastMaker;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

}
