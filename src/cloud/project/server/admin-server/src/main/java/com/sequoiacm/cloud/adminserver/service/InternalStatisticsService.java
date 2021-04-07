package com.sequoiacm.cloud.adminserver.service;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsFileRawData;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsRawData;

import java.util.List;

public interface InternalStatisticsService {

    void reportFileRawData(List<ScmStatisticsFileRawData> rawDataList) throws StatisticsException;
}
