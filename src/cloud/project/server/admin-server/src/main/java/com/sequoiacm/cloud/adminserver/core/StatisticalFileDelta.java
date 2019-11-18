package com.sequoiacm.cloud.adminserver.core;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

public class StatisticalFileDelta implements ScmStatistics {
    private static final Logger logger = LoggerFactory.getLogger(StatisticalFileDelta.class);

    public StatisticalFileDelta() {
    }

    @Override
    public void doStatistics(boolean needBacktrace) throws StatisticsException {
        List<WorkspaceInfo> workspaceList = StatisticsServer.getInstance().getWorkspaces();
        _execute(workspaceList, false, needBacktrace);
    }

    @Override
    public void refresh(WorkspaceInfo... workspaces) throws StatisticsException {
        _execute(Arrays.asList(workspaces), true, true);
    }

    private void _execute(List<WorkspaceInfo> workspaces, boolean isRefresh, boolean needBacktrace)
            throws StatisticsException {
        if (workspaces == null || workspaces.isEmpty()) {
            logger.warn("statistical traffic: no available workspaces");
            return;
        }

        List<ContentServerInfo> tmpServers = StatisticsServer.getInstance().getContentServers();
        
        for (WorkspaceInfo wsInfo : workspaces) {
            String wsName = wsInfo.getName();
            recordFileDelta(wsName, isRefresh, needBacktrace, tmpServers);
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
        FileDeltaInfo fileDelta = client.getFileDelta(wsName, condition, scope);
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
