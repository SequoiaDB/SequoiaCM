package com.sequoiacm.cloud.adminserver.service.impl;

import com.sequoiacm.cloud.adminserver.StatisticsConfig;
import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;
import com.sequoiacm.cloud.adminserver.dao.FileStatisticsDao;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.lock.LockPathFactory;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsData;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsDataKey;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsRawDataSum;
import com.sequoiacm.cloud.adminserver.service.InternalStatisticsService;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsFileRawData;
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
            if (data == null) {
                data = new FileStatisticsRawDataSum(key, 1,
                        fileRawData.getFileMeta().getTrafficSize(), fileRawData.getResponseTime());
                sumMap.put(key, data);
            }
            else {
                data.setRequestCount(data.getRequestCount() + 1);
                data.setTotalTrafficSize(
                        data.getTotalTrafficSize() + fileRawData.getFileMeta().getTrafficSize());
                data.setTotalResponseTime(
                        data.getTotalResponseTime() + fileRawData.getResponseTime());
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
                    long newAvgTrafficSize = (statisticsData.getAvgTrafficSize()
                            * statisticsData.getRequestCount() + sumRawData.getTotalTrafficSize())
                            / (statisticsData.getRequestCount() + sumRawData.getRequestCount());
                    long newAvgRespTime = (statisticsData.getAvgResponseTime()
                            * statisticsData.getRequestCount() + sumRawData.getTotalResponseTime())
                            / (statisticsData.getRequestCount() + sumRawData.getRequestCount());
                    int newReqCount = statisticsData.getRequestCount()
                            + sumRawData.getRequestCount();
                    statisticsData.setAvgTrafficSize(newAvgTrafficSize);
                    statisticsData.setAvgResponseTime(newAvgRespTime);
                    statisticsData.setRequestCount(newReqCount);
                    dao.saveFileStatisticData(sumRawData.getKey(), statisticsData);
                }
            }
            finally {
                lock.unlock();
            }
        }
    }

}
