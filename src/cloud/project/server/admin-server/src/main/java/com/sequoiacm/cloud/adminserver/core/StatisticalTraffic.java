package com.sequoiacm.cloud.adminserver.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cloud.adminserver.common.CommonUtils;
import com.sequoiacm.cloud.adminserver.common.StatisticsDefine;
import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.model.ContentServerInfo;
import com.sequoiacm.cloud.adminserver.model.TrafficInfo;
import com.sequoiacm.cloud.adminserver.model.WorkspaceInfo;
import com.sequoiacm.cloud.adminserver.remote.ContentServerClient;
import com.sequoiacm.cloud.adminserver.remote.ContentServerClientFactory;

public class StatisticalTraffic implements ScmStatistics {
    private static final Logger logger = LoggerFactory.getLogger(StatisticalTraffic.class);

    public StatisticalTraffic() {
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
        StatisticsServer statisticsServer = StatisticsServer.getInstance();

        if (workspaces == null || workspaces.isEmpty()) {
            logger.warn("statistical traffic: no available workspaces");
            return;
        }

        List<Map<String, Object>> metrics = new ArrayList<>();
        List<ContentServerInfo> contentServers = statisticsServer.getContentServers();
        for (ContentServerInfo serverInfo : contentServers) {
            Map<String, Object> metricInfo = accessRemoteServerMetrics(serverInfo, true);
            if (metricInfo != null) {
                metrics.add(metricInfo);
            }
        }

        recordTraffic(workspaces, metrics, StatisticsDefine.InterfaceType.FILE_UPLOAD,
                StatisticsDefine.METRICS_PREFIX_FILE_UPLOAD, isRefresh, needBacktrace);
        recordTraffic(workspaces, metrics, StatisticsDefine.InterfaceType.FILE_DOWNLOAD,
                StatisticsDefine.METRICS_PREFIX_FILE_DOWNLOAD, isRefresh, needBacktrace);
    }

    private Map<String, Object> accessRemoteServerMetrics(ContentServerInfo serverInfo,
            boolean isProcessException) throws StatisticsException {
        try {
            ContentServerClient client = ContentServerClientFactory
                    .getFeignClientByNodeUrl(serverInfo.getNodeUrl());
            Map<String, Object> metrics = client.metrics();
            logger.debug("access remote={},metrics={}", serverInfo.getNodeUrl(), metrics);
            return metrics;
        }
        catch (Exception e) {
            if (isProcessException) {
                logger.warn("access remote node failed:remote={}", serverInfo.getNodeUrl(), e);
            }
            else {
                throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                        "access remote node failed:remote=" + serverInfo.getNodeUrl(), e);
            }
        }
        return null;
    }

    private void recordTraffic(List<WorkspaceInfo> workspaces, List<Map<String, Object>> metrics,
            String trafficType, String prefix, boolean isRefresh, boolean needBacktrace)
            throws StatisticsException {
        StatisticsServer statisticsServer = StatisticsServer.getInstance();
        long traffic = 0;
        Date today = CommonUtils.getToday();
        Date yesterday = CommonUtils.getYesterday(today);
        // ensure date sequence and do not repeat
        Set<Date> dateSet = new LinkedHashSet<>();
        for (WorkspaceInfo ws : workspaces) {
            String wsName = ws.getName();
            dateSet.clear();

            // go back to historical records to fill in missing statistics.
            // for example, statistics service is down for a few days, it needs
            // to be backtracked when it start again.
            if (needBacktrace) {
                TrafficInfo record = statisticsServer.getLastTrafficRecord(trafficType, wsName);
                if (record != null) {
                    dateSet = CommonUtils.getDateRange(new Date(record.getRecordTime()), yesterday);
                }
            }

            dateSet.add(yesterday);

            if (isRefresh) {
                dateSet.add(today);
            }

            for (Date date : dateSet) {
                String dateStr = CommonUtils.formatCurrentDate(date);
                traffic = 0;
                for (Map<String, Object> metricsMap : metrics) {
                    Object val = metricsMap.get(prefix + wsName + "." + dateStr);
                    if (val != null) {
                        traffic += (int) val;
                    }
                }

                statisticsServer.upsertTrafficRecord(trafficType, wsName, date.getTime(), traffic);
                logger.debug(
                        "update traffic record success:type={},workspace={},time={},traffic={}",
                        trafficType, wsName, date.getTime(), traffic);
            }
        }
    }
}
