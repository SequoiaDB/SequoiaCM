package com.sequoiacm.metasource.sequoiadb.module;

import org.bson.BSONObject;

public class ScmRecyclingLog {

    private int siteId;
    private String dataSourceType;
    private BSONObject logInfo;
    private String time;

    public ScmRecyclingLog() {
    }

    public ScmRecyclingLog(int siteId, String dataSourceType, BSONObject logInfo, String time) {
        this.siteId = siteId;
        this.dataSourceType = dataSourceType;
        this.logInfo = logInfo;
        this.time = time;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public String getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(String dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    public BSONObject getLogInfo() {
        return logInfo;
    }

    public void setLogInfo(BSONObject logInfo) {
        this.logInfo = logInfo;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "SdbRecyclingLog{" + "siteId=" + siteId + ", dataSourceType='" + dataSourceType
                + '\'' + ", logInfo=" + logInfo + ", time='" + time + '\'' + '}';
    }
}
