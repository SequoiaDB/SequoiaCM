package com.sequoiacm.cloud.adminserver.model;

public class TrafficInfo {
    private String type;
    private String workspaceName;
    private long traffic;
    private long recordTime;
    
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getWorkspaceName() {
        return workspaceName;
    }
    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }
    public long getTraffic() {
        return traffic;
    }
    public void setTraffic(long traffic) {
        this.traffic = traffic;
    }
    public long getRecordTime() {
        return recordTime;
    }
    public void setRecordTime(long recordTime) {
        this.recordTime = recordTime;
    }
}
