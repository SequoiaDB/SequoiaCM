package com.sequoiacm.cloud.adminserver.dao;

import org.bson.BSONObject;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiacm.cloud.adminserver.model.FileDeltaInfo;
import com.sequoiacm.cloud.adminserver.model.TrafficInfo;

public interface StatisticsDao {
    TrafficInfo queryLastTrafficRecord(String type, String workspace)
            throws StatisticsException;

    FileDeltaInfo queryLastFileDeltaRecord(String workspace) throws StatisticsException;
    
    void upsertTraffic(String type, String workspace, long recordTime, long newTraffic)
            throws StatisticsException;
    
    void upsertFileDelta(String workspace, long recordTime, long newCount, long newSize)
            throws StatisticsException;
    
    MetaCursor getTrafficList(BSONObject filter) throws StatisticsException;
    
    MetaCursor getFileDeltaList(BSONObject filter) throws StatisticsException;
}
