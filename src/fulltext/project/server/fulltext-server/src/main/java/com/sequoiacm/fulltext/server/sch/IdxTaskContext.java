package com.sequoiacm.fulltext.server.sch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdxTaskContext {
    private static final Logger logger = LoggerFactory.getLogger(IdxTaskContext.class);
    private volatile long successCount;
    private volatile long errorCount;
    private volatile long totalTaskCount;

    public IdxTaskContext() {

    }

    public long getErrorCount() {
        return errorCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public synchronized void incSuccessCount(int count) {
        successCount += count;

    }

    public synchronized void incErrorCount(int count) {
        errorCount += count;
    }

    public synchronized void reduceTaskCount() {
        totalTaskCount--;
        if (totalTaskCount <= 0) {
            this.notify();
        }
    }

    public synchronized void waitAllTaskFinish() throws InterruptedException {
        while (true) {
            if (totalTaskCount <= 0) {
                return;
            }
            logger.debug("watiting for all background task exit");
            this.wait(20000);
        }
    }

    public synchronized boolean waitAllTaskFinish(long timeout) throws InterruptedException {
        if (totalTaskCount <= 0) {
            return true;
        }
        this.wait(timeout);

        if (totalTaskCount <= 0) {
            return true;
        }
        return false;
    }

    public synchronized void incTaskCount() {
        totalTaskCount++;
    }
}
