package com.sequoiacm.test.exec;

import com.sequoiacm.test.common.BsonUtil;
import com.sequoiacm.test.config.ScmTestToolProps;
import com.sequoiacm.test.module.ExecResult;
import com.sequoiacm.test.module.Worker;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class ScmTestProgress {

    private final static Logger logger = LoggerFactory.getLogger(ScmTestProgress.class);
    protected Worker worker;
    protected Future<ExecResult> future;
    protected int total = -1;
    protected int successCount;
    protected int failedCount;
    protected int skippedCount;
    private String runningTestcase;

    private boolean hasNewFailures;

    protected ScmTestProgress(Worker worker, Future<ExecResult> future) {
        this.worker = worker;
        this.future = future;
    }

    public int getTotal() {
        return total;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public boolean isComplete() {
        return future.isDone();
    }

    public void waitTestComplete(long time) throws Exception {
        try {
            future.get(time, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e) {
            // do nothing, just catch
        }
    }

    public boolean isExecSuccess() {
        ExecResult execResult;
        try {
            execResult = future.get();
            if (total == -1) {
                if (execResult.getExitCode() == 0) {
                    logger.error("Failed to read the test progress file. worker={}",
                            worker.getName());
                }
                else {
                    logger.error("Test exec failed, worker={}, detail: {}", worker.getName(),
                            execResult);
                }
            }
            else {
                logger.debug("The exec result of test, worker={}, detail:{}", worker.getName(),
                        execResult);
            }
        }
        catch (Exception e) {
            logger.warn("Failed to get the exec result, worker={}, cause by: {}", worker.getName(),
                    e.getMessage(), e);
        }

        return total != -1;
    }

    public boolean isPassAllTestcase() {
        return total == successCount + skippedCount;
    }

    public boolean isFailedCountChanged() {
        return hasNewFailures;
    }

    public void updateProgress() {
        BSONObject bson = readFromProgressFile();
        if (bson != null) {
            total = BsonUtil.getIntegerChecked(bson, "total");
            successCount = BsonUtil.getIntegerChecked(bson, "success");
            skippedCount = BsonUtil.getIntegerChecked(bson, "skipped");
            runningTestcase = BsonUtil.getStringChecked(bson, "running");

            int tmpFailedCount = BsonUtil.getIntegerChecked(bson, "failed");
            hasNewFailures = (tmpFailedCount > failedCount);
            failedCount = tmpFailedCount;
        }
    }

    protected abstract BSONObject readFromProgressFile();

    @Override
    public String toString() {
        if (total != -1) {
            String progress = getPaddingNameOfWorker(worker.getName()) + " |" + " total:" + total
                    + ", success:" + successCount + ", failed:" + failedCount + ", skipped:"
                    + skippedCount;
            if (runningTestcase.length() > 0) {
                return progress + ", running:" + runningTestcase;
            }
            return progress;
        }
        return getPaddingNameOfWorker(worker.getName()) + " | waiting to run... ";
    }

    private String getPaddingNameOfWorker(String hostname) {
        int maxLen = ScmTestToolProps.getInstance().getMaxLenOfHostname();
        StringBuilder sb = new StringBuilder(hostname);
        int i = hostname.length();
        while (i++ < maxLen) {
            sb.append(" ");
        }
        return sb.toString();
    }
}
