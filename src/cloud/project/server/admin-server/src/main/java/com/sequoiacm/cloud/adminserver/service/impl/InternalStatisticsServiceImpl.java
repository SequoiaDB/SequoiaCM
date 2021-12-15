package com.sequoiacm.cloud.adminserver.service.impl;

import com.sequoiacm.cloud.adminserver.StatisticsConfig;
import com.sequoiacm.cloud.adminserver.dao.BreakpointFileStatisticsDao;
import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;
import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.model.BreakpointFileStatisticsData;
import com.sequoiacm.cloud.adminserver.model.statistics.BreakpointFileStatisticsDataKey;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import com.sequoiacm.infrastructure.statistics.common.*;
import com.sequoiacm.cloud.adminserver.dao.FileStatisticsDao;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.lock.LockPathFactory;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsData;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsDataKey;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsRawDataSum;
import com.sequoiacm.cloud.adminserver.service.InternalStatisticsService;
import com.sequoiacm.infrastructure.lock.ScmLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InternalStatisticsServiceImpl implements InternalStatisticsService {
    @Autowired
    private StatisticsConfig config;
    @Autowired
    private ScmLockManager lockManger;
    @Autowired
    private LockPathFactory lockPathFactory;
    @Autowired
    private FileStatisticsDao dao;
    @Autowired
    private BreakpointFileStatisticsDao breakpointFileDao;
    
    private Logger logger = LoggerFactory.getLogger(InternalStatisticsServiceImpl.class);

    @Override
    public void reportFileRawData(List<ScmStatisticsFileRawData> rawDataList)
            throws StatisticsException {
        // 以 type+user+time+workspace 为 key，分别累加相同 key 的 rawData
        Map<FileStatisticsDataKey, FileStatisticsRawDataSum> sumMap = new HashMap<>();
        for (ScmStatisticsFileRawData fileRawData : rawDataList) {
            String timeStr = ScmTimeAccuracy.truncateTime(fileRawData.getTimestamp(),
                    config.getTimeGranularity());
            FileStatisticsDataKey key = new FileStatisticsDataKey(timeStr, fileRawData.getUser(),
                    fileRawData.getFileMeta().getWorkspace(), fileRawData.getType());
            FileStatisticsRawDataSum data = sumMap.get(key);
            long responseTime = fileRawData.getResponseTime();
            if (fileRawData.getFileMeta().getBreakpointFileName() != null
                    && fileRawData.isSuccess()) {
                ScmStatisticsFileMeta fileMeta = fileRawData.getFileMeta();
                responseTime += breakpointFileDao.getTotalUploadTime(
                        fileMeta.getBreakpointFileName(), fileMeta.getWorkspace(),
                        fileMeta.getDataCreateTime());
                breakpointFileDao.deleteBreakpointFileRecord(fileMeta.getBreakpointFileName(),
                        fileMeta.getWorkspace(), fileMeta.getDataCreateTime());
            }
            if (data == null) {
                if (fileRawData.isSuccess()) {
                    data = new FileStatisticsRawDataSum(key, 1, 0,
                            fileRawData.getFileMeta().getTrafficSize(), responseTime, responseTime,
                            responseTime);
                }
                else {
                    data = new FileStatisticsRawDataSum(key, 1, 1, 0, 0, Long.MIN_VALUE,
                            Long.MAX_VALUE);
                }
                sumMap.put(key, data);
            }
            else {
                data.setRequestCount(data.getRequestCount() + 1);
                // 失败的请求不参与文件流量大小、响应时间的统计
                if (fileRawData.isSuccess()) {
                    data.setTotalTrafficSize(data.getTotalTrafficSize()
                            + fileRawData.getFileMeta().getTrafficSize());
                    data.setTotalResponseTime(data.getTotalResponseTime() + responseTime);
                    data.setMaxResponseTime(Math.max(responseTime, data.getMaxResponseTime()));
                    data.setMinResponseTime(Math.min(responseTime, data.getMinResponseTime()));
                }
                else {
                    data.setFailCount(data.getFailCount() + 1);
                }

            }
        }

        // 按 type 分类累加后的 rawDataSum，顺便计算它的平均值
        Map<String, List<FileStatisticsRawDataSum>> type2SumDataList = new HashMap<>();
        for (FileStatisticsRawDataSum rawDataSum : sumMap.values()) {
            rawDataSum.calFileStatisticsData();
            List<FileStatisticsRawDataSum> list = type2SumDataList
                    .get(rawDataSum.getKey().getType());
            if (list == null) {
                list = new ArrayList<>();
                type2SumDataList.put(rawDataSum.getKey().getType(), list);
            }
            list.add(rawDataSum);
        }

        for (Map.Entry<String, List<FileStatisticsRawDataSum>> entry : type2SumDataList
                .entrySet()) {
            String type = entry.getKey();
            List<FileStatisticsRawDataSum> dataSumList = entry.getValue();
            ScmLock lock;
            try {
                lock = lockManger.acquiresLock(lockPathFactory.fileStatisticsLock(type));
            }
            catch (ScmLockException e) {
                throw new StatisticsException(StatisticsError.LOCK_ERROR,
                        "failed to acquires statistics lock:" + type, e);
            }
            try {
                for (FileStatisticsRawDataSum sumRawData : dataSumList) {
                    FileStatisticsData statisticsData = dao
                            .getFileStatisticData(sumRawData.getKey());
                    if (statisticsData == null) {
                        dao.saveFileStatisticData(sumRawData.getKey(),
                                sumRawData.getFileStatisticsData());
                        continue;
                    }
                    dao.saveFileStatisticData(sumRawData.getKey(),
                            calFileStatisticData(statisticsData, sumRawData));
                }
            }
            finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void reportBreakpointFileRawData(List<ScmStatisticsBreakpointFileRawData> rawDataList)
            throws StatisticsException {
        // 根据断点文件分类 : key为：filename+workspace+createTime , value对应文件总的上传时间
        Map<BreakpointFileStatisticsDataKey, Long> timeMap = new HashMap<>();
        for (ScmStatisticsBreakpointFileRawData rawData : rawDataList) {
            if (!rawData.isSuccess()) {
                continue;
            }
            ScmStatisticsBreakpointFileMeta fileMeta = rawData.getFileMeta();
            BreakpointFileStatisticsDataKey key = new BreakpointFileStatisticsDataKey(
                    fileMeta.getFileName(), fileMeta.getWorkspaceName(), fileMeta.getCreateTime());
            Long totalUploadTime = timeMap.get(key);
            if (totalUploadTime != null) {
                timeMap.put(key, totalUploadTime + rawData.getResponseTime());
            }
            else {
                timeMap.put(key, rawData.getResponseTime());
            }
        }

        for (Map.Entry<BreakpointFileStatisticsDataKey, Long> entry : timeMap.entrySet()) {
            BreakpointFileStatisticsDataKey key = entry.getKey();
            Long uploadTime = entry.getValue();
            try {
                BreakpointFileStatisticsData record = new BreakpointFileStatisticsData();
                record.setDataKey(key);
                record.setTotalUploadTime(uploadTime);
                breakpointFileDao.saveBreakpointFileRecord(record);
            }
            catch (ScmMetasourceException e) {
                if (StatisticsError.RECORD_EXISTS.equals(e.getCode())) {
                    breakpointFileDao.incrTotalUploadTime(key, uploadTime);
                }
                else {
                    throw e;
                }
            }

        }
    }

    private FileStatisticsData calFileStatisticData(FileStatisticsData statisticsData,
            FileStatisticsRawDataSum sumRawData) {

        int statisticsSuccessCount = statisticsData.getRequestCount()
                - statisticsData.getFailCount();
        int rawDataSuccessCount = sumRawData.getRequestCount() - sumRawData.getFailCount();
        long newAvgTrafficSize = 0;
        if ((statisticsSuccessCount + rawDataSuccessCount) > 0) {
            newAvgTrafficSize = (statisticsData.getAvgTrafficSize() * statisticsSuccessCount
                    + sumRawData.getTotalTrafficSize())
                    / (rawDataSuccessCount + statisticsSuccessCount);
        }
        long newAvgRespTime = 0;
        if ((statisticsSuccessCount + rawDataSuccessCount) > 0) {
            newAvgRespTime = (statisticsData.getAvgResponseTime() * statisticsSuccessCount
                    + sumRawData.getTotalResponseTime())
                    / (statisticsSuccessCount + rawDataSuccessCount);
        }
        int newReqCount = statisticsData.getRequestCount() + sumRawData.getRequestCount();
        int newFailCount = statisticsData.getFailCount() + sumRawData.getFailCount();

        long newMinResponseTime = 0;
        long newMaxResponseTime = 0;
        if (statisticsSuccessCount <= 0) {
            newMinResponseTime = sumRawData.getMinResponseTime();
            newMaxResponseTime = sumRawData.getMaxResponseTime();
        }
        else if (rawDataSuccessCount <= 0) {
            newMinResponseTime = statisticsData.getMinResponseTime();
            newMaxResponseTime = statisticsData.getMaxResponseTime();
        }
        else {
            newMinResponseTime = Math.min(statisticsData.getMinResponseTime(),
                    sumRawData.getMinResponseTime());
            newMaxResponseTime = Math.max(statisticsData.getMaxResponseTime(),
                    sumRawData.getMaxResponseTime());
        }

        FileStatisticsData newStatisticsData = new FileStatisticsData();
        newStatisticsData.setAvgTrafficSize(newAvgTrafficSize);
        newStatisticsData.setAvgResponseTime(newAvgRespTime);
        newStatisticsData.setRequestCount(newReqCount);
        newStatisticsData.setMinResponseTime(newMinResponseTime);
        newStatisticsData.setMaxResponseTime(newMaxResponseTime);
        newStatisticsData.setFailCount(newFailCount);
        return newStatisticsData;
    }

}
