package com.sequoiacm.infrastructure.statistics.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;

public class ScmStatisticsFileMeta {
    private long size;
    private String workspace;
    private String site;
    private String user;
    private String mimeType;
    private String version;
    private String batchId;
    private long trafficSize;

    public ScmStatisticsFileMeta() {
    }

    public ScmStatisticsFileMeta(String workspace, String site, String user, String mimeType,
            String version, String batchId, long size, long trafficSize) {
        this.workspace = workspace;
        this.site = site;
        this.user = user;
        this.mimeType = mimeType;
        this.version = version;
        this.batchId = batchId;
        this.size = size;
        this.trafficSize = trafficSize;
    }

    public long getTrafficSize() {
        return trafficSize;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public void setTrafficSize(long trafficSize) {
        this.trafficSize = trafficSize;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public String toJSON() {
        return JSON.toJSONString(this);
    }

    @Override
    public String toString() {
        return "ScmStatisticsFileMeta{" + "workspace='" + workspace + '\'' + ", site='" + site
                + '\'' + ", user='" + user + '\'' + ", mimeType='" + mimeType + '\'' + ", version='"
                + version + '\'' + ", batchId='" + batchId + '\'' + ", size=" + size
                + ", trafficSize=" + trafficSize + ", toJSON='" + toJSON() + '\'' + '}';
    }

    public static ScmStatisticsFileMeta fromJSON(String json) {
        return JSON.parseObject(json, ScmStatisticsFileMeta.class, Feature.IgnoreNotMatch);
    }

}
