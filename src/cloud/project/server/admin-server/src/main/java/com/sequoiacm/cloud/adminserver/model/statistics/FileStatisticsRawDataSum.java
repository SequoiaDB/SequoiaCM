package com.sequoiacm.cloud.adminserver.model.statistics;

public class FileStatisticsRawDataSum {
    private FileStatisticsDataKey key;
    private int requestCount;
    private long totalTrafficSize;
    private long totalResponseTime;
    private FileStatisticsData fileStatisticsData;

    public FileStatisticsRawDataSum(FileStatisticsDataKey key, int requestCount, long totalTrafficSize,
            long totalResponseTime) {
        this.requestCount = requestCount;
        this.totalTrafficSize = totalTrafficSize;
        this.totalResponseTime = totalResponseTime;
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
                + ", totalTrafficSize=" + totalTrafficSize + ", totalResponseTime=" + totalResponseTime
                + '}';
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

    public void calFileStatisticsData() {
        this.fileStatisticsData = new FileStatisticsData(requestCount, totalTrafficSize / requestCount,
                totalResponseTime / requestCount);
    }

    public FileStatisticsData getFileStatisticsData() {
        if (fileStatisticsData == null) {
            throw new IllegalStateException("call calFileStatisticsData first");
        }
        return fileStatisticsData;
    }
}
