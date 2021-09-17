package com.sequoiacm.infrastructure.statistics.common;

public class ScmStatisticsFileRawData extends ScmStatisticsRawData {
    private ScmStatisticsFileMeta fileMeta;

    public ScmStatisticsFileRawData() {
    }

    public ScmStatisticsFileRawData(boolean isSuccess, String type, String user, long timestamp,
            long responseTime, String extra) {
        super(isSuccess, type, user, timestamp, responseTime);
        this.fileMeta = ScmStatisticsFileMeta.fromJSON(extra);
    }

    public ScmStatisticsFileMeta getFileMeta() {
        return fileMeta;
    }

    public void setFileMeta(ScmStatisticsFileMeta fileMeta) {
        this.fileMeta = fileMeta;
    }

    @Override
    public String toString() {
        return "ScmStatisticsFileRawData{" + "fileMeta=" + fileMeta + ", isSuccess=" + isSuccess()
                + ", user='" + getUser() + '\'' + ", timestamp=" + getTimestamp()
                + ", responseTime=" + getResponseTime() + ", type='" + getType() + '\'' + '}';
    }
}
