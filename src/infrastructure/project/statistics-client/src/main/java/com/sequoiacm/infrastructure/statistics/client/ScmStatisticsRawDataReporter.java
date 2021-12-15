package com.sequoiacm.infrastructure.statistics.client;

import java.util.*;
import java.util.concurrent.*;

import javax.annotation.PreDestroy;

import com.sequoiacm.infrastructure.statistics.client.flush.StatisticsRawDataFlush;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsRawData;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsRawDataFactory;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsType;

@RefreshScope
public class ScmStatisticsRawDataReporter {
    private static final Logger logger = LoggerFactory
            .getLogger(ScmStatisticsRawDataReporter.class);

    private final ReporterJob reporterJob;

    private final Queue<ScmStatisticsRawData> cache;
    private final int cacheSizeLimit;
    private final int highWatermark;
    private final ExecutorService executors;
    private ScmStatisticsClient client;
    private List<StatisticsRawDataFlush> rawDataFlushList = new ArrayList<>();

    public ScmStatisticsRawDataReporter(ScmStatisticsReporterConfig config,
            ScmStatisticsClient client, List<StatisticsRawDataFlush> rawDataFlushList) {
        cache = new ConcurrentLinkedQueue<>();
        cacheSizeLimit = config.getRawDataCacheSize();
        highWatermark = cacheSizeLimit / 5;
        this.client = client;
        executors = Executors.newSingleThreadExecutor(
                new CustomizableThreadFactory("scm-statistics-rawdata-reporter"));
        reporterJob = new ReporterJob(cache, config.getRawDataReportPeriod(), highWatermark,
                client);
        if (rawDataFlushList != null) {
            this.rawDataFlushList = rawDataFlushList;
        }

        // 执行一次空数据的上报，让 feign 相关的懒加载 bean 初始化起来，
        // 这样做是为了避免在 destroy() 中出现首次上报数据的情况，spring context 销毁阶段已经无法初始化 feign 相关实例
        reporterJob.reportRawData(ScmStatisticsType.FILE_DOWNLOAD,
                Collections.<ScmStatisticsRawData> emptyList());

        executors.submit(reporterJob);
    }

    @PreDestroy
    @Order(Integer.MIN_VALUE)
    public void destroy() throws InterruptedException {
        if (reporterJob != null) {
            reporterJob.stopAndWaitExit();
        }
        if (executors != null) {
            executors.shutdown();
        }
    }

    public void report(boolean isSuccess, String type, String user, long timestamp,
            long responseTime, String extra) {
        ScmStatisticsRawData rawData = ScmStatisticsRawDataFactory.createRawData(isSuccess, type,
                user, timestamp, responseTime, extra);
        logger.debug("add raw data to cache queue: {}", rawData);
        report(rawData);
    }

    public void report(ScmStatisticsRawData rawData) {
        cache.offer(rawData);
        for (StatisticsRawDataFlush statisticsRawDataFlush : rawDataFlushList) {
            try {
                if (statisticsRawDataFlush.isNeedFlush(rawData)) {
                    ScmStatisticsFlushCondition flushCondition = statisticsRawDataFlush
                            .getFlushCondition(rawData);
                    flushSelectedRawData(flushCondition);
                }
            }
            catch (Exception e) {
                logger.warn("failed to flush statistics raw data", e);
            }
        }
        if (cache.size() >= highWatermark) {
            wakeupReporterJob();
        }
        if (cache.size() > cacheSizeLimit) {
            ScmStatisticsRawData ret = cache.poll();
            if (ret != null) {
                logger.debug("cache queue is full, discard oldest raw data: {}", ret);
            }
        }
    }

    public void flushSelectedRawData(ScmStatisticsFlushCondition condition) {
        Map<String, List<ScmStatisticsRawData>> type2RawData = new HashMap<>();
        for (ScmStatisticsRawData rawData : cache) {
            if (condition.isMatch(rawData)) {
                boolean removed = cache.remove(rawData);
                if (removed) {
                    List<ScmStatisticsRawData> rawDataList = type2RawData.get(rawData.getType());
                    if (rawDataList == null) {
                        rawDataList = new ArrayList<>();
                        type2RawData.put(rawData.getType(), rawDataList);
                    }
                    rawDataList.add(rawData);
                }
            }
        }
        for (Map.Entry<String, List<ScmStatisticsRawData>> entry : type2RawData.entrySet()) {
            try {
                client.reportRawData(entry.getKey(), entry.getValue());
            }
            catch (Exception e) {
                logger.warn(
                        "failed to flush statistics raw data to admin-server:type={}, count={}",
                        entry.getKey(), entry.getValue().size(), e);
            }
        }
    }

    private void wakeupReporterJob() {
        reporterJob.wakeup();
    }

    public interface ScmStatisticsFlushCondition {
        boolean isMatch(ScmStatisticsRawData statisticsRawData);
    }
}

class ReporterJob implements Runnable {
    private final Queue<ScmStatisticsRawData> rawDataQueue;
    private final int period;
    private final int REPORT_BATCH_SIZE = 50;
    private final ScmStatisticsClient client;
    private final int highWatermark;
    private volatile boolean exitFlag;
    private final CountDownLatch exitCountdownLatch;
    private static final Logger logger = LoggerFactory.getLogger(ReporterJob.class);
    private Map<String, List<ScmStatisticsRawData>> lastFailedReportData;
    private boolean isLastReportFailed = false;

    public ReporterJob(Queue<ScmStatisticsRawData> rawDataQueue, int period,
            int highWatermark, ScmStatisticsClient client) {
        this.rawDataQueue = rawDataQueue;
        this.period = period;
        this.client = client;
        this.exitFlag = false;
        this.exitCountdownLatch = new CountDownLatch(1);
        this.highWatermark = highWatermark;

    }

    @Override
    public void run() {
        while (true) {
            try {
                if (exitFlag) {
                    logger.info("Reporter exiting, reporting the remaining raw data right now: {}",
                            rawDataQueue.size());
                    reportAllData();
                    exitCountdownLatch.countDown();
                    logger.info("Reporter exited");
                    return;
                }

                // 队列数据目前非常少，或上一次发送数据失败了，都需要等一段时间再上报
                if (rawDataQueue.size() < REPORT_BATCH_SIZE || isLastReportFailed) {
                    waitIgnoreException(period);
                }

                boolean isHighWater = rawDataQueue.size() > highWatermark;

                // 上一次有上报失败的数据，先处理它
                if (lastFailedReportData != null) {
                    lastFailedReportData = reportRawData(lastFailedReportData);
                    if (lastFailedReportData != null) {
                        isLastReportFailed = true;
                        if (isHighWater) {
                            // 队列已经到达了高水位，不再保留上一次发送失败的数据
                            lastFailedReportData = null;
                            logger.warn(
                                    "discard failed reported raw data, cause by high watermark:highWatermark={}",
                                    highWatermark);
                        }
                        continue;
                    }
                    isLastReportFailed = false;
                }

                int countBatch = rawDataQueue.size() / REPORT_BATCH_SIZE;
                for (int i = 0; i <= countBatch; i++) {
                    lastFailedReportData = reportRawData(REPORT_BATCH_SIZE);
                    if (lastFailedReportData != null) {
                        isLastReportFailed = true;
                        break;
                    }
                }
                isLastReportFailed = false;
            }
            catch (Throwable t) {
                logger.error("reporter catch unexpected error!", t);
            }
        }

    }

    void reportAllData() {
        if (lastFailedReportData != null) {
            reportRawData(lastFailedReportData);
        }
        while (rawDataQueue.size() > 0) {
            reportRawData(REPORT_BATCH_SIZE);
        }
    }

    private synchronized void waitIgnoreException(long t) {
        try {
            this.wait(t);
        }
        catch (InterruptedException e) {
            logger.warn("reporter catch interrupted exception", e);
        }
    }

    boolean reportRawData(String type, List<ScmStatisticsRawData> data) {
        logger.debug("reporting raw data: type={}, data={}", type, data);
        try {
            client.reportRawData(type, data);
            return true;
        }
        catch (Exception e) {
            logger.warn("failed to report statistics raw data to admin-server:type={}, count={}",
                    type, data.size(), e);
        }
        return false;
    }

    // 全部发送成功返回null，否则返回发送失败的数据
    Map<String, List<ScmStatisticsRawData>> reportRawData(
            Map<String, List<ScmStatisticsRawData>> type2RawData) {
        Iterator<Map.Entry<String, List<ScmStatisticsRawData>>> it = type2RawData.entrySet()
                .iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<ScmStatisticsRawData>> entry = it.next();
            boolean isSuccess = reportRawData(entry.getKey(), entry.getValue());
            if (!isSuccess) {
                return type2RawData;
            }
            it.remove();
        }
        return null;
    }

    // 发送成功返回null，否则返回发送失败的数据
    Map<String, List<ScmStatisticsRawData>> reportRawData(int size) {
        Map<String, List<ScmStatisticsRawData>> type2RawData = new HashMap<>();
        while (size-- > 0) {
            ScmStatisticsRawData rawData = rawDataQueue.poll();
            if (rawData == null) {
                break;
            }
            List<ScmStatisticsRawData> rawDataList = type2RawData.get(rawData.getType());
            if (rawDataList == null) {
                rawDataList = new ArrayList<>();
                type2RawData.put(rawData.getType(), rawDataList);
            }
            rawDataList.add(rawData);
        }
        return reportRawData(type2RawData);
    }

    public void stopAndWaitExit() throws InterruptedException {
        exitFlag = true;
        wakeup();
        exitCountdownLatch.await();
    }

    public synchronized void wakeup() {
        this.notify();
    }
}