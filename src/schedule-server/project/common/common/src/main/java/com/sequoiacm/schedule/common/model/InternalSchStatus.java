package com.sequoiacm.schedule.common.model;

import org.bson.BSONObject;

public class InternalSchStatus {
    private String schId;
    private String schName;
    private String workerNode;
    private long startTime;
    private boolean finish;
    private BSONObject status;

    public String getSchName() {
        return schName;
    }

    public void setSchName(String schName) {
        this.schName = schName;
    }

    public InternalSchStatus() {
    }

    public boolean isFinish() {
        return finish;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
    }

    public String getSchId() {
        return schId;
    }

    public void setSchId(String schId) {
        this.schId = schId;
    }

    public String getWorkerNode() {
        return workerNode;
    }

    public void setWorkerNode(String workerNode) {
        this.workerNode = workerNode;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public BSONObject getStatus() {
        return status;
    }

    public void setStatus(BSONObject status) {
        this.status = status;
    }

}
