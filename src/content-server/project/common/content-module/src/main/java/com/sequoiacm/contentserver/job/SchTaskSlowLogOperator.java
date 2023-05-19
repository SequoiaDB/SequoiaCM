package com.sequoiacm.contentserver.job;

import com.sequoiacm.infrastructure.slowlog.NoOpSlowLogContextImpl;
import com.sequoiacm.infrastructure.slowlog.SlowLogContext;
import com.sequoiacm.infrastructure.slowlog.SlowLogContextImpl;
import com.sequoiacm.infrastructure.slowlog.SlowLogManager;
import com.sequoiacm.infrastructure.slowlog.appender.LoggerFileAppender;
import com.sequoiacm.infrastructure.slowlog.appender.SlowLogAppender;
import com.sequoiacm.infrastructure.slowlog.config.SlowLogConfig;
import com.sequoiacm.infrastructure.slowlog.module.OperationStatistics;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SchTaskSlowLogOperator {

    private static SlowLogConfig slowLogConfig;

    private static final SchTaskSlowLogOperator schTaskSlowLogOperator = new SchTaskSlowLogOperator();
    private static final ThreadLocal<SimpleDateFormat> dataFormatLocal = new ThreadLocal<>();

    private SchTaskSlowLogOperator() {
    }

    public static void init(SlowLogConfig conf) {
        slowLogConfig = conf;
    }

    public static SchTaskSlowLogOperator getInstance() {
        return schTaskSlowLogOperator;
    }

    public void doTaskBefore() {
        if (slowLogConfig.isEnabled()) {
            SlowLogContext slowLogContext = new SlowLogContextImpl();
            SlowLogManager.setCurrentContext(slowLogContext);
            slowLogContext.setStart(new Date());
        }
    }

    public void doTaskAfter(String taskId, String fileId) {
        SlowLogContext logContext = SlowLogManager.getCurrentContext();
        if (logContext instanceof NoOpSlowLogContextImpl) {
            SlowLogManager.setCurrentContext(null);
            return;
        }
        logContext.setEnd(new Date());
        SlowResult slowResult = getSlowResult(logContext);
        if (slowResult != null && slowResult.isSlow()) {
            logResult(slowResult, logContext, taskId, fileId);
        }
        SlowLogManager.setCurrentContext(null);
    }

    private SlowResult getSlowResult(SlowLogContext logContext) {
        if (!slowLogConfig.isEnabled()) {
            return null;
        }
        SlowResult slowResult = null;
        // 检查操作是否超时
        if (slowLogConfig.isOperationConfigured()) {
            slowResult = new SlowResult();
            for (OperationStatistics statistics : logContext.getOperationStatisticsData()) {
                long threshold = slowLogConfig.getOperationThreshold(statistics.getName());
                if (statistics.getSpend() >= threshold) {
                    slowResult.setSlow(true);
                    slowResult.addSlowOperation(statistics.getName());
                }
            }
        }
        return slowResult;
    }

    private void logResult(SlowResult slowResult, SlowLogContext logContext, String taskId,
            String fileId) {
        if (slowLogConfig.getAppenderList().size() <= 0) {
            return;
        }
        SlowLogAppender logAppender = null;
        for (SlowLogAppender appender : slowLogConfig.getAppenderList()) {
            // 只输出到日志文件
            if (appender instanceof LoggerFileAppender) {
                logAppender = appender;
                break;
            }
        }
        if (null == logAppender) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("schedule file is too slow: spend=").append(logContext.getSpend())
                .append("ms, details=");
        sb.append("[");
        sb.append("taskId=").append(taskId).append(", ");
        sb.append("spend=").append(logContext.getSpend()).append("ms, ");
        sb.append("fileId=").append(fileId).append(", ");
        sb.append("start=").append(formatDate(logContext.getStart())).append(", ");
        sb.append("end=").append(formatDate(logContext.getEnd())).append(", ");
        Collection<OperationStatistics> statisticsData = logContext.getOperationStatisticsData();
        if (statisticsData != null && statisticsData.size() > 0) {
            sb.append(", operations=[");
            int n = statisticsData.size();
            for (OperationStatistics statistics : statisticsData) {
                String operation = statistics.getName();
                if (slowResult.isSlowOperation(operation)) {
                    sb.append("*");
                }
                sb.append(operation);
                if (statistics.getCount() > 1) {
                    sb.append("(").append(statistics.getCount()).append(")");
                }
                sb.append("=").append(statistics.getSpend()).append("ms");
                if (--n > 0) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        }
        Map<String, Set<Object>> extras = logContext.getExtras();
        if (extras != null && extras.size() > 0) {
            sb.append(", extras=[");
            int n = extras.size();
            for (Map.Entry<String, Set<Object>> extraEntry : extras.entrySet()) {
                sb.append(extraEntry.getKey()).append("=");
                if (extraEntry.getValue().size() == 1) {
                    sb.append(extraEntry.getValue().iterator().next());
                }
                else {
                    sb.append(extraEntry.getValue());
                }
                if (--n > 0) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        }
        sb.append("]");
        logAppender.log(sb.toString());
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat dateFormat = dataFormatLocal.get();
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            dataFormatLocal.set(dateFormat);
        }
        return dateFormat.format(date);
    }

    static class SlowResult {

        private List<String> slowOperations;
        private boolean slow = false;

        public SlowResult() {
        }

        public void addSlowOperation(String operation) {
            if (slowOperations == null) {
                slowOperations = new ArrayList<>();
            }
            slowOperations.add(operation);
        }

        public boolean isSlowOperation(String operation) {
            if (slowOperations == null) {
                return false;
            }
            return slowOperations.contains(operation);
        }

        public boolean isSlow() {
            return slow;
        }

        public void setSlow(boolean slow) {
            this.slow = slow;
        }
    }
}
