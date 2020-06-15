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
    private String formatedPrefix;
    private String formatedstartAfter;

    public S3ListObjContextMeta() {
    }

    public S3ListObjContextMeta(String id, String bucketName, String ws, String bucketDir,
            String prefix, String startAfter, String delimiter, String lastMarker) {
        this.id = id;
        this.ws = ws;
        this.bucketName = bucketName;
        this.bucketDir = bucketDir;
        this.prefix = prefix;
        this.formatedPrefix = formatPath(prefix);

        this.startAfter = startAfter;
        this.formatedstartAfter = formatPath(startAfter);
        this.delimiter = delimiter;
       
        this.lastMarker = formatPath(lastMarker);
        this.updateTime = System.currentTimeMillis();
    }

    private String formatPath(String p) {
        if (p != null && !p.startsWith(S3CommonDefine.SCM_DIR_SEP)) {
            p = S3CommonDefine.SCM_DIR_SEP + p;
        }
        return p;
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

    public String getFormatedPrefix() {
        return formatedPrefix;
    }

    public void setFormatedPrefix(String formatedPrefix) {
        this.formatedPrefix = formatedPrefix;
    }

    public void setFormatedstartAfter(String formatedstartAfter) {
        this.formatedstartAfter = formatedstartAfter;
    }

    public String getFormatedstartAfter() {
        return formatedstartAfter;
    }

    public String getStartAfter() {
        return startAfter;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void updateLastMarker(String lastMaker) {
        this.lastMarker = formatPath(lastMaker);
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setStartAfter(String startAfter) {
        this.startAfter = startAfter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public void setLastMarker(String lastMarker) {
        this.lastMarker = lastMarker;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public void setWs(String ws) {
        this.ws = ws;
    }

    public void setBucketDir(String bucketDir) {
        this.bucketDir = bucketDir;
    }

    public void setId(String id) {
        this.id = id;
    }

}
