package com.sequoiacm.diagnose.task;

import java.util.concurrent.atomic.AtomicLong;

public class ExecutionContext {
    private volatile String status = Status.NORMAL;
    private volatile Exception exception;
    private AtomicLong incompleteTaskCount = new AtomicLong(0);

    public ExecutionContext() {
    }

    public boolean isNormal() {
        return status.equals(Status.NORMAL);
    }

    public Exception exception() {
        return exception;
    }

    public long getIncompleteTaskCount() {
        return incompleteTaskCount.get();
    }

    public void addTask() {
        incompleteTaskCount.incrementAndGet();
    }

    public void taskCompleted() {
        incompleteTaskCount.decrementAndGet();
    }

    public void setHasException(Exception e) {
        status = Status.EXCEPTIONAL;
        exception = e;
    }

    public static class Status {
        public static final String NORMAL = "NORMAL";
        public static final String EXCEPTIONAL = "EXCEPTIONAL";
    }
}
