package com.sequoiacm.cloud.adminserver.model.statistics;

public class FileStatisticsRawDataSum {
    private FileStatisticsDataKey key;
    private int requestCount;
    private int failCount;
    private long totalTrafficSize;
    private long totalResponseTime;
    private long maxResponseTime;
    private long minResponseTime;
    private FileStatisticsData fileStatisticsData;

    public FileStatisticsRawDataSum(FileStatisticsDataKey key, int requestCount, int failCount,
            long totalTrafficSize, long totalResponseTime, long maxResponseTime,
            long minResponseTime) {
        this.requestCount = requestCount;
        this.failCount = failCount;
        this.totalTrafficSize = totalTrafficSize;
        this.totalResponseTime = totalResponseTime;
        this.maxResponseTime = maxResponseTime;
        this.minResponseTime = minResponseTime;
        this.key = key;
    }

    public FileStatisticsDataKey getKey() {
        return key;
    }

    public void setKey(FileStatisticsDataKey key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "FileStatisticsRawDataSum{" + "key=" + key + ", requestCount=" + requestCount
                + ", failCount=" + failCount + ", totalTrafficSize=" + totalTrafficSize
                + ", totalResponseTime=" + totalResponseTime + ", maxResponseTime="
                + maxResponseTime + ", minResponseTime=" + minResponseTime + '}';
    }

    public int getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
    }

    public long getTotalTrafficSize() {
        return totalTrafficSize;
    }

    public void setTotalTrafficSize(long totalTrafficSize) {
        this.totalTrafficSize = totalTrafficSize;
    }

    public long getTotalResponseTime() {
        return totalResponseTime;
    }

    public void setTotalResponseTime(long totalResponseTime) {
        this.totalResponseTime = totalResponseTime;
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

    public void calFileStatisticsData() {
        int successCount = requestCount - failCount;
        long avgTrafficSize = successCount <= 0 ? 0 : totalTrafficSize / successCount;
        long avgResponseTime = successCount <= 0 ? 0 : totalResponseTime / successCount;
        long maxResTime = successCount <= 0 ? 0 : maxResponseTime;
        long minResTime = successCount <= 0 ? 0 : minResponseTime;
        this.fileStatisticsData = new FileStatisticsData(requestCount, avgTrafficSize,
                avgResponseTime, maxResTime, minResTime, failCount);
    }

    public FileStatisticsData getFileStatisticsData() {
        if (fileStatisticsData == null) {
            throw new IllegalStateException("call calFileStatisticsData first");
        }
        return fileStatisticsData;
    }
}
