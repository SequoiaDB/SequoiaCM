package com.sequoiacm.perf.vo;

public class RecordVo {
    private String threadName;
    private long fileSize;
    private int fileNum;
    private long total;
    private long average;

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getFileNum() {
        return fileNum;
    }

    public void setFileNum(int fileNum) {
        this.fileNum = fileNum;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getAverage() {
        return average;
    }

    public void setAverage(long average) {
        this.average = average;
    }
}
