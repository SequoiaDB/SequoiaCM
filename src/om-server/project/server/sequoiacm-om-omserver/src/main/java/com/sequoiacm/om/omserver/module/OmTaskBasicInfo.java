package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class OmTaskBasicInfo {
    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("success_count")
    private Long successCount;

    @JsonProperty("actual_count")
    private Long actualCount;

    @JsonProperty("fail_count")
    private Long failCount;

    @JsonProperty("start_time")
    private Date startTime;

    @JsonProperty("stop_time")
    private Date stopTime;

    @JsonProperty("progress")
    private Integer progress;

    @JsonProperty("status")
    private Integer status;

    public OmTaskBasicInfo() {
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Long successCount) {
        this.successCount = successCount;
    }

    public Long getActualCount() {
        return actualCount;
    }

    public void setActualCount(Long actualCount) {
        this.actualCount = actualCount;
    }

    public Long getFailCount() {
        return failCount;
    }

    public void setFailCount(Long failCount) {
        this.failCount = failCount;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStopTime() {
        return stopTime;
    }

    public void setStopTime(Date stopTime) {
        this.stopTime = stopTime;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
