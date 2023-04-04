package com.sequoiacm.diagnose.progress;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Progress {
    protected long totalCount;
    protected AtomicLong successCount = new AtomicLong(0);
    protected AtomicLong failureCount = new AtomicLong(0);
    protected AtomicLong progressCount = new AtomicLong(0);

    public long getTotalCount() {
        return totalCount;
    }

    public AtomicLong getFailureCount() {
        return failureCount;
    }

    public void success(long count) {
        successCount.getAndAdd(count);
        progressCount.getAndAdd(count);
    }

    public void failed(long count) {
        failureCount.getAndAdd(count);
        progressCount.getAndAdd(count);
    }

    public AtomicLong getProgressCount() {
        return progressCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }
}
