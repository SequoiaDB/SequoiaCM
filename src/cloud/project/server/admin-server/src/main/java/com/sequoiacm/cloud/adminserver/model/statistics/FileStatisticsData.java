package com.sequoiacm.cloud.adminserver.model.statistics;

public class FileStatisticsData {
    private int requestCount;
    private long avgTrafficSize;
    private long avgResponseTime;

    public FileStatisticsData() {
    }

    public FileStatisticsData(int requestCount, long avgTrafficSize, long avgResponseTime) {
        this.requestCount = requestCount;
        this.avgTrafficSize = avgTrafficSize;
        this.avgResponseTime = avgResponseTime;
    }

    @Override
    public String toString() {
        return "FileStatisticsData{" + "requestCount=" + requestCount + ", avgTrafficSize="
                + avgTrafficSize + ", avgResponseTime=" + avgResponseTime + '}';
    }

    public int getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
    }

    public long getAvgTrafficSize() {
        return avgTrafficSize;
    }

    public void setAvgTrafficSize(long avgTrafficSize) {
        this.avgTrafficSize = avgTrafficSize;
    }

    public long getAvgResponseTime() {
        return avgResponseTime;
    }

    public void setAvgResponseTime(long avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

}
