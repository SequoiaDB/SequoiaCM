package com.sequoiacm.cloud.adminserver.service;

import org.bson.BSONObject;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;

public interface StatisticsService {

    void refresh(int type, String wsName) throws StatisticsException;
    
    MetaCursor getTrafficList(BSONObject filter) throws StatisticsException;
    
    MetaCursor getFileDeltaList(BSONObject filter) throws StatisticsException;
}
