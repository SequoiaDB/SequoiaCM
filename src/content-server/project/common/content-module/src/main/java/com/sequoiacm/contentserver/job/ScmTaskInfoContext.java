package com.sequoiacm.contentserver.job;

import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.dataoperation.ScmSpaceRecyclingInfo;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class ScmTaskInfoContext {

    private final static Logger logger = LoggerFactory.getLogger(ScmTaskInfoContext.class);

    private final ScmTaskBase task;

    private final AtomicLong activeCount = new AtomicLong();
    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failedCount = new AtomicLong();

    private Date preDate = new Date();
    private static final int DATE_STEP = 20; // seconds
    private int preProgress = 0;
    private static final int PROGRESS_STEP = 10;
    private long estimateCount = -1;
    private long actualCount = -1;

    private boolean aborted;
    private Throwable abortException;

    private BSONObject recentRecycleExtraInfo;

    public ScmTaskInfoContext(ScmTaskBase task) {
        this.task = task;
    }

    public void incrementActiveCount() {
        activeCount.incrementAndGet();
    }

    public void decrementActiveCount() {
        activeCount.decrementAndGet();
    }

    public void incrementSuccessCount() {
        successCount.incrementAndGet();
        updateProgress(getSuccessCount(), getFailedCount());
    }

    public void setSuccessCount(int count) {
        successCount.set(count);
        updateProgress(getSuccessCount(), getFailedCount());
    }

    public void setFailedCount(int count) {
        failedCount.set(count);
        updateProgress(getSuccessCount(), getFailedCount());
    }

    public void incrementFailedCount() {
        failedCount.incrementAndGet();
        updateProgress(getSuccessCount(), getFailedCount());
    }

    public void subTaskAbort(Throwable abortException) {
        this.aborted = true;
        this.abortException = abortException;
    }

    public void subTaskFinish(ScmDoFileRes res) {
        if (res == ScmDoFileRes.SUCCESS) {
            incrementSuccessCount();
        }
        else if (res == ScmDoFileRes.FAIL) {
            incrementFailedCount();
        }
        if (res == ScmDoFileRes.SKIP || res == ScmDoFileRes.INTERRUPT) {
            // there is no special operation,ignore it
        }

    }

    public void reduceActiveCount(int count) {
        activeCount.addAndGet(-count);
    }

    public void waitAllSubTaskFinish() throws Exception {
        waitAllSubTaskFinish(Long.MAX_VALUE, 100, null);
    }

    public void waitAllSubTaskFinish(long maxWaitTime, long waitInterval, WaitCallback callback)
            throws Exception {
        long beginTime = System.currentTimeMillis();
        int waitingCount = 0;
        long waitingTime = 0;
        while (activeCount.get() > 0) {
            Thread.sleep(waitInterval);
            waitingTime = System.currentTimeMillis() - beginTime;
            if (waitingTime >= maxWaitTime) {
                logger.warn("wait task finish timeout, waitingTime={}, maxWaitTime={}, taskId={}",
                        waitingTime, maxWaitTime, task.getTaskId());
                break;
            }
            waitingCount++;
            if (callback != null && !callback.shouldContinueWait(waitingTime, waitingCount)) {
                return;
            }
        }
    }

    private void updateProgress(long successCount, long failedCount) {
        try {
            Date date = new Date();
            int seconds = ScmSystemUtils.getDuration(preDate, date);

            int progress = calculateProgress(successCount, failedCount);

            if ((progress != -1 && (progress - preProgress >= PROGRESS_STEP))
                    || seconds > DATE_STEP) {
                ScmContentModule.getInstance().getMetaService().updateTaskProgress(task.getTaskId(),
                        progress, successCount, failedCount);

                preProgress = progress;
                preDate = date;
            }
        }
        catch (Exception e) {
            logger.warn("updateProgress failed", e);
        }
    }

    private int calculateProgress(long successCount, long failedCount) {
        if (actualCount == -1) {
            return -1;
        }
        int progress = 0;
        if (actualCount > 0) {
            progress = (int) (100 * ((double) (successCount + failedCount) / actualCount));
        }

        if (progress >= 100) {
            progress = 99;
        }
        return progress;
    }

    public int getProgress() {
        return calculateProgress(successCount.get(), failedCount.get());
    }

    public long getEstimateCount() {
        return estimateCount;
    }

    public void setEstimateCount(long estimateCount) {
        this.estimateCount = estimateCount;
    }

    public long getActualCount() {
        return actualCount;
    }

    public void setActualCount(long actualCount) {
        this.actualCount = actualCount;
    }

    public Date getPreDate() {
        return preDate;
    }

    public void setPreDate(Date preDate) {
        this.preDate = preDate;
    }

    public ScmTaskBase getTask() {
        return task;
    }

    public boolean isAborted() {
        return aborted;
    }

    public Throwable getAbortException() {
        return abortException;
    }

    public long getSuccessCount() {
        return successCount.get();
    }

    public long getActiveCount() {
        return activeCount.get();
    }

    public long getFailedCount() {
        return failedCount.get();
    }

    public void recordExtraInfo(ScmSpaceRecyclingInfo recyclingInfo) {
        if (recyclingInfo == null || recyclingInfo.getInfo() == null) {
            return;
        }
        try {
            if (recentRecycleExtraInfo == null) {
                recentRecycleExtraInfo = recyclingInfo.getInfo();
                ScmContentModule.getInstance().getMetaService()
                        .updateTaskExtraInfo(task.getTaskId(), recentRecycleExtraInfo);
            }
            else if (!recentRecycleExtraInfo.equals(recyclingInfo.getInfo())) {
                recentRecycleExtraInfo = recyclingInfo.getInfo();
                ScmContentModule.getInstance().getMetaService()
                        .updateTaskExtraInfo(task.getTaskId(), recentRecycleExtraInfo);
            }
        }
        catch (ScmServerException e) {
            logger.error("failed to update task extraInfo, taskId={}", task.getTaskId(), e);
        }

    }

    public static interface WaitCallback {
        boolean shouldContinueWait(long waitingTime, int waitingCount) throws ScmServerException;
    }
}
