package com.sequoiacm.cloud.adminserver.service.impl;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.cloud.adminserver.common.StatisticsDefine;
import com.sequoiacm.cloud.adminserver.core.ScmStatistics;
import com.sequoiacm.cloud.adminserver.core.StatisticalFileDelta;
import com.sequoiacm.cloud.adminserver.core.StatisticalTraffic;
import com.sequoiacm.cloud.adminserver.core.StatisticsServer;
import com.sequoiacm.cloud.adminserver.dao.StatisticsDao;
import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiacm.cloud.adminserver.model.WorkspaceInfo;
import com.sequoiacm.cloud.adminserver.service.StatisticsService;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private StatisticsDao statisticsDao;
    
    @Override
    public void refresh(int type, String wsName) throws StatisticsException {
        WorkspaceInfo workspaceInfo = StatisticsServer.getInstance().getWorkspaceChecked(wsName);
        ScmStatistics statis = null;
        if (type == StatisticsDefine.StatisticsType.TRAFFIC) {
            statis = new StatisticalTraffic();
        }
        else if (type == StatisticsDefine.StatisticsType.FILE_DELTA) {
            statis = new StatisticalFileDelta();
        }
        else {
            throw new StatisticsException(StatisticsError.INVALID_ARGUMENT,
                    "unknown statistics type: type=" + type);
        }
        
        statis.refresh(workspaceInfo);
    }

    @Override
    public MetaCursor getTrafficList(BSONObject filter) throws StatisticsException {
        return statisticsDao.getTrafficList(filter);
    }

    @Override
    public MetaCursor getFileDeltaList(BSONObject filter) throws StatisticsException {
        return statisticsDao.getFileDeltaList(filter);
    }
}
