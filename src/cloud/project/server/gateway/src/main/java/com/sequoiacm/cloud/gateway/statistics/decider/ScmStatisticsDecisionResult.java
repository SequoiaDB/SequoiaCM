package com.sequoiacm.cloud.gateway.statistics.decider;

public class ScmStatisticsDecisionResult {
    private final boolean isNeedStatistics;
    private final String statisticsType;

    public ScmStatisticsDecisionResult(boolean isNeedStatistics, String statisticsType) {
        this.isNeedStatistics = isNeedStatistics;
        this.statisticsType = statisticsType;
    }

    public boolean isNeedStatistics() {
        return isNeedStatistics;
    }

    public String getStatisticsType() {
        return statisticsType;
    }

    @Override public String toString() {
        return "ScmStatisticsDecisionResult{" + "isNeedStatistics=" + isNeedStatistics
                + ", statisticsType='" + statisticsType + '\'' + '}';
    }
}
