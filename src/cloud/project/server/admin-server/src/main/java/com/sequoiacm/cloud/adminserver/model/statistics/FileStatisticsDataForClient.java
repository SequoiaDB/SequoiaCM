package com.sequoiacm.cloud.adminserver.model.statistics;

public class FileStatisticsDataForClient {
    private final FileStatisticsData statisticsData;
    private final FileStatisticsDataQueryCondition condition;

    public FileStatisticsDataForClient(FileStatisticsData statisticsData,
            FileStatisticsDataQueryCondition condition) {
        this.statisticsData = statisticsData;
        this.condition = condition;
    }

    @Override
    public String toString() {
        return "FileStatisticsDataForClient{" + "statisticsData=" + statisticsData + ", condition="
                + condition + '}';
    }

    public FileStatisticsData getStatisticsData() {
        return statisticsData;
    }

    public FileStatisticsDataQueryCondition getCondition() {
        return condition;
    }
}
