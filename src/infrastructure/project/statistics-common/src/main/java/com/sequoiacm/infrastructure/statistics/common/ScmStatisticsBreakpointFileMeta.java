package com.sequoiacm.infrastructure.statistics.common;

import com.google.gson.Gson;

import java.util.Objects;

public class ScmStatisticsBreakpointFileMeta {
    private static final Gson gson = new Gson();

    private String fileName;
    private String workspaceName;
    private long createTime;
    private boolean isComplete;

    public ScmStatisticsBreakpointFileMeta() {
    }

    public ScmStatisticsBreakpointFileMeta(String fileName, String workspaceName, long createTime,
            boolean isComplete) {
        this.fileName = fileName;
        this.workspaceName = workspaceName;
        this.createTime = createTime;
        this.isComplete = isComplete;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public static ScmStatisticsBreakpointFileMeta fromJSON(String json) {
        return gson.fromJson(json, ScmStatisticsBreakpointFileMeta.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScmStatisticsBreakpointFileMeta that = (ScmStatisticsBreakpointFileMeta) o;
        return createTime == that.createTime && Objects.equals(fileName, that.fileName)
                && Objects.equals(workspaceName, that.workspaceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, workspaceName, createTime);
    }

    @Override
    public String toString() {
        return "ScmStatisticsBreakpointFileMeta{" + "fileName='" + fileName + '\''
                + ", workspaceName='" + workspaceName + '\'' + ", createTime=" + createTime
                + ", isComplete=" + isComplete + '}';
    }

    public String toJSON() {
        return gson.toJson(this);
    }
}
