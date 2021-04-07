package com.sequoiacm.cloud.adminserver.dao;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsData;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsDataKey;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsDataQueryCondition;

public interface FileStatisticsDao {
    FileStatisticsData getFileStatisticData(FileStatisticsDataKey key)
            throws StatisticsException;

    void saveFileStatisticData(FileStatisticsDataKey keyInfo, FileStatisticsData data)
            throws StatisticsException;

    FileStatisticsData getFileStatisticData(String fileStatisticType,
            FileStatisticsDataQueryCondition condition) throws StatisticsException;

}
