package com.sequoiacm.schedule.entity;

import org.bson.BSONObject;

public class TaskEntity {
    private String id;
    private int type;
    private String workspace;
    private BSONObject content;
    private int serverId;
    private Integer targetSite;
    private int progress;
    private int runningFlag;
    private long startTime;

    // init state stopTime must be null !!!! (file service depends on this)
    private Long stopTime = null;

    private long estimateCount;
    private long actualCount;
    private long successCount;
    private long failCount;

    private String scheduleId;

    private int scope;
    private long maxExecTime;

    private BSONObject option;
    private BSONObject extraInfo;

    public long getMaxExecTime() {
        return maxExecTime;
    }

    public void setMaxExecTime(long maxExecTime) {
        this.maxExecTime = maxExecTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public BSONObject getContent() {
        return content;
    }

    public void setContent(BSONObject content) {
        this.content = content;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }
    
    public Integer getTargetSite() {
        return targetSite;
    }

    public void setTargetSite(Integer targetSite) {
        this.targetSite = targetSite;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getRunningFlag() {
        return runningFlag;
    }

    public void setRunningFlag(int runningFlag) {
        this.runningFlag = runningFlag;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Long getStopTime() {
        return stopTime;
    }

    public void setStopTime(Long stopTime) {
        this.stopTime = stopTime;
    }

    public Long getEstimateCount() {
        return estimateCount;
    }

    public void setEstimateCount(long estimateCount) {
        this.estimateCount = estimateCount;
    }

    public long getActualCount() {
        return actualCount;
    }

    public void setActualCount(long actualCount) {
        this.actualCount = actualCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public long getFailCount() {
        return failCount;
    }

    public void setFailCount(long failCount) {
        this.failCount = failCount;
    }

    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }

    public int getScope() {
        return scope;
    }

    public BSONObject getOption() {
        return option;
    }

    public void setOption(BSONObject option) {
        this.option = option;
    }

    public BSONObject getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(BSONObject extraInfo) {
        this.extraInfo = extraInfo;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id:").append(getId()).append(",").append("scheduleId:").append(getScheduleId())
        .append(",").append("type:").append(getType()).append(",").append("workspace:")
        .append(getWorkspace()).append(",").append("serverId:").append(getServerId())
        .append(",").append("runningFlag:").append(getRunningFlag()).append(",")
        .append("startTime:").append(getStartTime()).append(",").append("content:")
        .append(getContent()).append(",").append("estimateCount:")
        .append(getEstimateCount()).append(",").append("actulCount:")
        .append(getActualCount()).append(",").append("sucessCount:")
        .append(getSuccessCount()).append(",").append("failCount:").append(getFailCount())
                .append(",").append("scope:").append(getScope()).append(",").append("option:")
                .append(getOption()).append(",").append("extraInfo:").append(getExtraInfo());
        return sb.toString();
    }
}
