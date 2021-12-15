package com.sequoiacm.cloud.adminserver.model;

import com.sequoiacm.cloud.adminserver.model.statistics.BreakpointFileStatisticsDataKey;

public class BreakpointFileStatisticsData {

    private BreakpointFileStatisticsDataKey dataKey;
    private long totalUploadTime;


    public long getTotalUploadTime() {
        return totalUploadTime;
    }

    public void setTotalUploadTime(long totalUploadTime) {
        this.totalUploadTime = totalUploadTime;
    }

    public BreakpointFileStatisticsDataKey getDataKey() {
        return dataKey;
    }

    public void setDataKey(BreakpointFileStatisticsDataKey dataKey) {
        this.dataKey = dataKey;
    }
}
