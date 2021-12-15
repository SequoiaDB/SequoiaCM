package com.sequoiacm.infrastructure.statistics.common;

public class ScmStatisticsBreakpointFileRawData extends ScmStatisticsRawData {
    private ScmStatisticsBreakpointFileMeta fileMeta;

    public ScmStatisticsBreakpointFileRawData(boolean isSuccess, String type, String user,
            long timestamp, long responseTime, String fileMeta) {
        super(isSuccess, type, user, timestamp, responseTime);
        this.fileMeta = ScmStatisticsBreakpointFileMeta.fromJSON(fileMeta);
    }

    public ScmStatisticsBreakpointFileMeta getFileMeta() {
        return fileMeta;
    }

    public void setFileMeta(ScmStatisticsBreakpointFileMeta fileMeta) {
        this.fileMeta = fileMeta;
    }

    public ScmStatisticsBreakpointFileRawData() {
    }

    @Override
    public String toString() {
        return "ScmStatisticsBreakpointFileRawData{" + "fileMeta=" + fileMeta + ", isSuccess="
                + isSuccess() + ", user='" + getUser() + '\'' + ", timestamp=" + getTimestamp()
                + ", responseTime=" + getResponseTime() + ", type='" + getType() + '\'' + '}';
    }
}
