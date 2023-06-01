package com.sequoiacm.cloud.adminserver.core;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmLockPath;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cloud.adminserver.common.CommonUtils;
import com.sequoiacm.cloud.adminserver.common.FieldName;
import com.sequoiacm.cloud.adminserver.common.SequoiadbHelper;
import com.sequoiacm.cloud.adminserver.common.StatisticsDefine;
import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.model.ContentServerInfo;
import com.sequoiacm.cloud.adminserver.model.FileDeltaInfo;
import com.sequoiacm.cloud.adminserver.model.WorkspaceInfo;
import com.sequoiacm.cloud.adminserver.remote.ContentServerClient;
import com.sequoiacm.cloud.adminserver.remote.ContentServerClientFactory;
import org.springframework.http.HttpStatus;

public class StatisticalFileDelta implements ScmStatistics {
    private static final Logger logger = LoggerFactory.getLogger(StatisticalFileDelta.class);

    private static final long MIN_STATISTICS_INTERVAL = 1000 * 60 * 30L; // 30 minutes

    public StatisticalFileDelta() {
    }

    @Override
    public void doStatistics(boolean needBacktrace) throws StatisticsException {
        List<WorkspaceInfo> workspaceList = StatisticsServer.getInstance().getWorkspaces();
        _execute(workspaceList, false, needBacktrace, false);
    }

    @Override
    public void refresh(WorkspaceInfo... workspaces) throws StatisticsException {
        _execute(Arrays.asList(workspaces), true, true, true);
    }

    private void _execute(List<WorkspaceInfo> workspaces, boolean isRefresh, boolean needBacktrace,
            boolean forceUpdate)
            throws StatisticsException {
        if (workspaces == null || workspaces.isEmpty()) {
            logger.warn("statistical traffic: no available workspaces");
            return;
        }

        Map<Integer, List<ContentServerInfo>> allServersMap = CommonUtils.getAllServersMap();

        for (WorkspaceInfo wsInfo : workspaces) {
            String wsName = wsInfo.getName();
            List<ContentServerInfo> conformServers = CommonUtils
                    .getConformServers(wsInfo.getSiteList(), allServersMap);
            if (forceUpdate) {
                recordFileDelta(wsName, isRefresh, needBacktrace, conformServers);
            }
            else {
                ScmLock lock = null;
                try {
                    StatisticsServer statisticsServer = StatisticsServer.getInstance();
                    ScmLockPath lockPath = statisticsServer.getLockPathFactory()
                            .fileDeltaStatisticsLock(wsName);
                    lock = statisticsServer.getLockManager().acquiresLock(lockPath);
                    FileDeltaInfo lastFileDeltaRecord = statisticsServer
                            .getLastFileDeltaRecord(wsName);
                    if (lastFileDeltaRecord != null && (System.currentTimeMillis()
                            - lastFileDeltaRecord.getUpdateTime() < MIN_STATISTICS_INTERVAL)) {
                        logger.info(
                                "statistics have been performed recently, skip it:workspace={},lastUpdateTime={}",
                                wsName, lastFileDeltaRecord.getUpdateTime());
                    }
                    else {
                        recordFileDelta(wsName, isRefresh, needBacktrace, conformServers);
                    }
                }
                catch (ScmLockException e) {
                    throw new StatisticsException(StatisticsError.LOCK_ERROR,
                            "failed to acquire file statistics lock", e);
                }
                finally {
                    if (lock != null) {
                        lock.unlock();
                    }
                }
            }
        }
    }

    private void recordFileDelta(String wsName, boolean isRefresh, boolean needBacktrace,
            List<ContentServerInfo> tmpServers) throws StatisticsException {
        StatisticsServer statisticsServer = StatisticsServer.getInstance();
        // ensure date sequence and do not repeat
        Set<Date> dateSet = new LinkedHashSet<>();
        Date today = CommonUtils.getToday();
        Date yesterday = CommonUtils.getYesterday(today);

        // go back to historical records to fill in missing statistics.
        // for example, statistics service is down for a few days, it needs
        // to be backtracked when it start again.
        if (needBacktrace) {
            FileDeltaInfo record = statisticsServer.getLastFileDeltaRecord(wsName);
            if (record != null) {
                dateSet = CommonUtils.getDateRange(new Date(record.getRecordTime()), yesterday);
            }
        }

        dateSet.add(yesterday);

        if (isRefresh) {
            dateSet.add(today);
        }

        BasicBSONObject and = new BasicBSONObject();
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.File.FIELD_CREATE_TIME, and);

        for (Date date : dateSet) {
            matcher.put(FieldName.File.FIELD_CREATE_MONTH, CommonUtils.getCurrentMonth(date));
            and.put(SequoiadbHelper.DOLLAR_GTE, date.getTime());
            and.put(SequoiadbHelper.DOLLAR_LT, CommonUtils.getTomorrow(date).getTime());

            FileDeltaInfo fileDelta = getFileDeltaInfoRandomly(tmpServers, wsName, matcher,
                    StatisticsDefine.Scope.SCOPE_CURRENT, true);
            long count = fileDelta.getCountDelta();
            long size = fileDelta.getSizeDelta();
            statisticsServer.upsertFileDeltaRecord(wsName, date.getTime(), count, size);
            logger.debug("update file delta record success:workspace={},time={},count={},size={}",
                    wsName, date.getTime(), count, size);
        }
    }

    private FileDeltaInfo getFileDeltaInfo(ContentServerInfo contentServer, String wsName,
            BSONObject condition, int scope) throws Exception {
        ContentServerClient client = ContentServerClientFactory
                .getFeignClientByNodeUrl(contentServer.getNodeUrl());
        FileDeltaInfo fileDelta = null;
        BSONObject res = null;

        // 优先走有连接保活的新接口，如果是旧节点，则走旧接口
        try {
            res = client.getFileDeltaKeepAlive(wsName, condition, scope);
        }
        catch (StatisticsException e) {
            if (!HttpStatus.BAD_REQUEST.getReasonPhrase().equalsIgnoreCase(e.getCode())) {
                throw e;
            }
        }

        if (res != null) {
            ScmFeignExceptionUtils.handleException(res);
            fileDelta = new FileDeltaInfo();
            fileDelta.setCountDelta(BsonUtils
                    .getNumberChecked(res, FieldName.FileDelta.FIELD_COUNT_DELTA).longValue());
            fileDelta.setSizeDelta(BsonUtils
                    .getNumberChecked(res, FieldName.FileDelta.FIELD_SIZE_DELTA).longValue());
        }
        else {
            fileDelta = client.getFileDeltaWithHead(wsName, condition, scope);
        }

        fileDelta.setWorkspaceName(wsName);
        logger.debug(
                "access remote success:{},wsName={},filter={},scope={},count_delta={},size_delta={}",
                contentServer.getNodeUrl(), wsName, condition, scope, fileDelta.getCountDelta(),
                fileDelta.getSizeDelta());
        return fileDelta;
    }

    private FileDeltaInfo getFileDeltaInfoRandomly(List<ContentServerInfo> serverInfos,
            String wsName, BSONObject condition, int scope, boolean isProcessException)
            throws StatisticsException {
        ContentServerInfo randomServer = CommonUtils.getRandomElement(serverInfos);
        while (randomServer != null) {
            try {
                FileDeltaInfo info = getFileDeltaInfo(randomServer, wsName, condition, scope);
                return info;
            }
            catch (Exception e) {
                if (!isProcessException) {
                    throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                            "access remote node failed:remote=" + randomServer.getNodeUrl(), e);
                }
                // remove exception server
                serverInfos.remove(randomServer);
                logger.warn("access remote node failed:remote={}",
                        null == randomServer ? null : randomServer.getNodeUrl(), e);
            }

            randomServer = CommonUtils.getRandomElement(serverInfos);
        }

        throw new StatisticsException(StatisticsError.INTERNAL_ERROR, "no accessible remote node");
    }
}
