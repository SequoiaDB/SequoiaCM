package com.sequoiacm.infrastructure.statistics.common;

public class ScmStatisticsRawDataFactory {
    public static ScmStatisticsRawData createRawData(boolean isSuccess, String type, String user,
            long timestamp, long responseTime, String extraInfo) {
        switch (type) {
            case ScmStatisticsType.FILE_DOWNLOAD:
            case ScmStatisticsType.FILE_UPLOAD:
                return new ScmStatisticsFileRawData(isSuccess, type, user, timestamp, responseTime,
                        extraInfo);
            default:
                throw new IllegalArgumentException("unrecognized raw data type:" + type);
        }
    }
}
