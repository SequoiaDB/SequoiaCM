package com.sequoiacm.s3import.common;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.config.ImportToolProps;
import com.sequoiacm.s3import.module.S3ImportBatch;
import com.sequoiacm.s3import.task.S3ImportTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class S3ImportBatchRunner {

    private static final Logger logger = LoggerFactory.getLogger(S3ImportBatchRunner.class);

    private ExecutorService executor;
    private long startTime;
    private long runStartTime;
    private long failureCount = 0;

    public S3ImportBatchRunner() throws ScmToolsException {
        int workCount = ImportToolProps.getInstance().getWorkCount();
        this.executor = Executors.newFixedThreadPool(workCount);
        this.startTime = System.currentTimeMillis();
    }

    public void runAndWaitBatchFinish(S3ImportBatch batch) {
        runStartTime = System.currentTimeMillis();
        List<Future<?>> futureList = new ArrayList<>();
        for (S3ImportTask task : batch.getTaskList()) {
            futureList.add(executor.submit(task));
        }

        for (Future<?> future : futureList) {
            try {
                future.get();
            }
            catch (Exception e) {
                logger.error("Run task failed, cause by:", e);
                batch.setHasAbortedTask(true);
            }
        }
    }

    public long getFailureCount() {
        return failureCount;
    }

    public void increaseFailCount(long count) {
        this.failureCount += count;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getRunStartTime() {
        return runStartTime;
    }

    public void stop() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
