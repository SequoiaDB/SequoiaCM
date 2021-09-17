package com.sequoiacm.infrastructure.statistics.common;

import org.bson.BSONObject;

import java.util.Date;

public class ScmStatisticsRawData {

    // 本次请求是否成功
    private boolean isSuccess = true;
    private String type;
    private String user;

    // 发起时间
    private long timestamp;

    // 耗时
    private long responseTime;

    public ScmStatisticsRawData() {
    }

    public ScmStatisticsRawData(boolean isSuccess, String type, String user, long timestamp,
            long responseTime) {
        this.isSuccess = isSuccess;
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

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    @Override
    public String toString() {
        return "ScmStatisticsRawData{" + "isSuccess=" + isSuccess + ", type='" + type + '\''
                + ", user='" + user + '\'' + ", timestamp=" + timestamp + ", responseTime="
                + responseTime + '}';
    }
}
