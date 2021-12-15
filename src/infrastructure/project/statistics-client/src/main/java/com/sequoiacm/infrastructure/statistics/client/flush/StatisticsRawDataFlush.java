package com.sequoiacm.infrastructure.statistics.client.flush;

import com.sequoiacm.infrastructure.statistics.client.ScmStatisticsRawDataReporter;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsRawData;

public interface StatisticsRawDataFlush {

    boolean isNeedFlush(ScmStatisticsRawData rawData);

    ScmStatisticsRawDataReporter.ScmStatisticsFlushCondition getFlushCondition(
            ScmStatisticsRawData rawData);

}
