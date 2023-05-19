package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.BSONObject;

import java.util.Date;

public class OmTaskBasicInfo {
    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("workspace")
    private String workspace;

    @JsonProperty("success_count")
    private Long successCount;

    @JsonProperty("estimate_count")
    private Long estimateCount;

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

    @JsonProperty("content")
    private BSONObject content;

    @JsonProperty("extra_info")
    private BSONObject extraInfo;

    @JsonProperty("detail")
    private String detail;

    @JsonProperty("start_execute_time")
    private Date startExecuteTime;

    public OmTaskBasicInfo() {
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Long successCount) {
        this.successCount = successCount;
    }

    public Long getEstimateCount() {
        return estimateCount;
    }

    public void setEstimateCount(Long estimateCount) {
        this.estimateCount = estimateCount;
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

    public BSONObject getContent() {
        return content;
    }

    public void setContent(BSONObject content) {
        this.content = content;
    }

    public BSONObject getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(BSONObject extraInfo) {
        this.extraInfo = extraInfo;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Date getStartExecuteTime() {
        return startExecuteTime;
    }

    public void setStartExecuteTime(Date startExecuteTime) {
        this.startExecuteTime = startExecuteTime;
    }
}
