package com.sequoiacm.cloud.adminserver.service;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsBreakpointFileRawData;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsFileRawData;

import java.util.List;

public interface InternalStatisticsService {

    void reportFileRawData(List<ScmStatisticsFileRawData> rawDataList) throws StatisticsException;

    void reportBreakpointFileRawData(List<ScmStatisticsBreakpointFileRawData> rawDataList)
            throws StatisticsException;
}
