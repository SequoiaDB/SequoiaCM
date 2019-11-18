package com.sequoiacm.cloud.adminserver.model;

public class FileDeltaInfo {
    private String workspaceName;
    private long countDelta;
    private long sizeDelta;
    private long recordTime;
    
    public String getWorkspaceName() {
        return workspaceName;
    }
    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }
    public long getCountDelta() {
        return countDelta;
    }
    public void setCountDelta(long countDelta) {
        this.countDelta = countDelta;
    }
    public long getSizeDelta() {
        return sizeDelta;
    }
    public void setSizeDelta(long sizeDelta) {
        this.sizeDelta = sizeDelta;
    }
    public long getRecordTime() {
        return recordTime;
    }
    public void setRecordTime(long recordTime) {
        this.recordTime = recordTime;
    }
}
