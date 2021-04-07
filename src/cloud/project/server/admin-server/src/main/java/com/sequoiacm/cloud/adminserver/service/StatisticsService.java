package com.sequoiacm.cloud.adminserver.service;

import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsData;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsDataQueryCondition;
import org.bson.BSONObject;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;

public interface StatisticsService {

    void refresh(int type, String wsName) throws StatisticsException;
    
    MetaCursor getTrafficList(BSONObject filter) throws StatisticsException;
    
    MetaCursor getFileDeltaList(BSONObject filter) throws StatisticsException;

    FileStatisticsData getFileStatistics(String fileStatisticsType, FileStatisticsDataQueryCondition condition)throws StatisticsException;
}
