package com.sequoiacm.infrastructure.statistics.common;

import org.bson.BSONObject;

import java.util.Date;


public class ScmStatisticsRawData {
    private String type;
    private String user;

    // 发起时间
    private long timestamp;

    // 耗时
    private long responseTime;

    public ScmStatisticsRawData() {
    }

    public ScmStatisticsRawData(String type, String user, long timestamp, long responseTime) {
        this.type = type;
        this.user = user;
        this.timestamp = timestamp;
        this.responseTime = responseTime;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ScmStatisticsRawData{" + "type='" + type + '\'' + ", user='" + user + '\''
                + ", timestamp=" + timestamp + ", responseTime=" + responseTime + '}';
    }
}
