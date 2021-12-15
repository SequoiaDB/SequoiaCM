package com.sequoiacm.cloud.adminserver.model.statistics;

import java.util.Objects;

public class BreakpointFileStatisticsDataKey {

    private String fileName;
    private String workspaceName;
    private long createTime;

    public BreakpointFileStatisticsDataKey(String fileName, String workspaceName, long createTime) {
        this.fileName = fileName;
        this.workspaceName = workspaceName;
        this.createTime = createTime;
    }

    public BreakpointFileStatisticsDataKey() {
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

    @Override
    public String toString() {
        return "BreakpointFileStatisticsDataKey{" + "fileName='" + fileName + '\''
                + ", workspaceName='" + workspaceName + '\'' + ", createTime=" + createTime + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BreakpointFileStatisticsDataKey that = (BreakpointFileStatisticsDataKey) o;
        return createTime == that.createTime && Objects.equals(fileName, that.fileName)
                && Objects.equals(workspaceName, that.workspaceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, workspaceName, createTime);
    }
}
