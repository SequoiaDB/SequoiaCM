package com.sequoiacm.infrastructure.statistics.common;

public class ScmStatisticsFileRawData extends ScmStatisticsRawData {
    private ScmStatisticsFileMeta fileMeta;

    public ScmStatisticsFileRawData() {
    }

    public ScmStatisticsFileRawData(String type, String user, long timestamp, long responseTime,
            String extra) {
        super(type, user, timestamp, responseTime);
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
        return "ScmStatisticsFileRawData{" + "fileMeta=" + fileMeta + ", user='" + getUser() + '\''
                + ", timestamp=" + getTimestamp() + ", responseTime=" + getResponseTime()
                + ", type='" + getType() + '\'' + '}';
    }
}
