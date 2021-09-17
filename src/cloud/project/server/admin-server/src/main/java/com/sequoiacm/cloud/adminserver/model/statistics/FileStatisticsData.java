package com.sequoiacm.cloud.adminserver.model.statistics;

public class FileStatisticsData {
    private int requestCount;
    private long avgTrafficSize;
    private long avgResponseTime;
    private long maxResponseTime;
    private long minResponseTime;
    private int failCount;

    public FileStatisticsData() {
    }

    public FileStatisticsData(int requestCount, long avgTrafficSize, long avgResponseTime) {
        this.requestCount = requestCount;
        this.avgTrafficSize = avgTrafficSize;
        this.avgResponseTime = avgResponseTime;
    }

    public FileStatisticsData(int requestCount, long avgTrafficSize, long avgResponseTime,
            long maxResponseTime, long minResponseTime, int failCount) {
        this.requestCount = requestCount;
        this.avgTrafficSize = avgTrafficSize;
        this.avgResponseTime = avgResponseTime;
        this.maxResponseTime = maxResponseTime;
        this.minResponseTime = minResponseTime;
        this.failCount = failCount;
    }

    @Override
    public String toString() {
        return "FileStatisticsData{" + "requestCount=" + requestCount + ", avgTrafficSize="
                + avgTrafficSize + ", avgResponseTime=" + avgResponseTime + ", maxResponseTime="
                + maxResponseTime + ", minResponseTime=" + minResponseTime + ", failCount="
                + failCount + '}';
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

    public long getMaxResponseTime() {
        return maxResponseTime;
    }

    public void setMaxResponseTime(long maxResponseTime) {
        this.maxResponseTime = maxResponseTime;
    }

    public long getMinResponseTime() {
        return minResponseTime;
    }

    public void setMinResponseTime(long minResponseTime) {
        this.minResponseTime = minResponseTime;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }
}
