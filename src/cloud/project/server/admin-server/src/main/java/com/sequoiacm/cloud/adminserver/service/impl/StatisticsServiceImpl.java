package com.sequoiacm.cloud.adminserver.service.impl;

import com.sequoiacm.cloud.adminserver.StatisticsConfig;
import com.sequoiacm.cloud.adminserver.common.FieldMatchChecker;
import com.sequoiacm.cloud.adminserver.core.StatisticsObjectDelta;
import com.sequoiacm.cloud.adminserver.dao.FileStatisticsDao;
import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsData;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsDataQueryCondition;
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

import java.util.regex.Pattern;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private StatisticsDao statisticsDao;

    @Autowired
    private FileStatisticsDao fileStatisticsDao;

    @Autowired
    private StatisticsConfig config;

    private final String datePattern = "^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$";

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
    public void refreshObjectDelta(String bucketName) throws StatisticsException {
        new StatisticsObjectDelta().refresh(bucketName);
    }

    @Override
    public MetaCursor getTrafficList(BSONObject filter) throws StatisticsException {
        return statisticsDao.getTrafficList(filter);
    }

    @Override
    public MetaCursor getFileDeltaList(BSONObject filter) throws StatisticsException {
        return statisticsDao.getFileDeltaList(filter);
    }

    @Override
    public MetaCursor getObjectDeltaList(BSONObject filter) throws StatisticsException {
        FieldMatchChecker.checkFields(filter, FieldMatchChecker.CheckType.OBJECT_DELTA);
        return statisticsDao.getObjectDeltaList(filter);
    }

    @Override
    public FileStatisticsData getFileStatistics(String fileStatisticsType,
            FileStatisticsDataQueryCondition condition) throws StatisticsException {
        if (condition.getBegin() == null) {
            throw new StatisticsException(StatisticsError.BAD_REQUEST,
                    "missing required field in condition: begin");
        }
        if (condition.getEnd() == null) {
            throw new StatisticsException(StatisticsError.BAD_REQUEST,
                    "missing required field in condition: end");
        }
        if (!Pattern.matches(datePattern, condition.getBegin())) {
            throw new StatisticsException(StatisticsError.BAD_REQUEST, "invalid date format:begin="
                    + condition.getBegin() + ", expected format is 'yyyy-MM-dd HH-mm-ss'");
        }
        if (!Pattern.matches(datePattern, condition.getEnd())) {
            throw new StatisticsException(StatisticsError.BAD_REQUEST, "invalid date format:end="
                    + condition.getEnd() + ", expected format is 'yyyy-MM-dd HH-mm-ss'");
        }
        if (condition.getEnd().compareTo(condition.getBegin()) <= 0) {
            throw new StatisticsException(StatisticsError.BAD_REQUEST,
                    "invalid condition, end must great than begin:end=" + condition.getEnd()
                            + ", begin=" + condition.getBegin());
        }
        String newBegin;
        String newEnd;
        if (condition.getTimeAccuracy() != null) {
            newBegin = ScmTimeAccuracy.truncateTime(condition.getBegin(),
                    condition.getTimeAccuracy());
            newEnd = ScmTimeAccuracy.truncateTime(condition.getEnd(),
                    condition.getTimeAccuracy());
        }
        else {
            newBegin = ScmTimeAccuracy.truncateTime(condition.getBegin(),
                    config.getTimeGranularity());
            newEnd = ScmTimeAccuracy.truncateTime(condition.getEnd(),
                    config.getTimeGranularity());
        }
        FileStatisticsDataQueryCondition newCondition = new FileStatisticsDataQueryCondition(condition.getUser(),
                condition.getWorkspace(), newBegin, newEnd, condition.getTimeAccuracy());
        return fileStatisticsDao.getFileStatisticData(fileStatisticsType, newCondition);
    }

}
