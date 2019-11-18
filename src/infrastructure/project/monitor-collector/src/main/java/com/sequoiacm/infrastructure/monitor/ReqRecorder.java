package com.sequoiacm.infrastructure.monitor;

import java.util.concurrent.atomic.AtomicLong;

public class ReqRecorder {

    private static ReqRecorder container = new ReqRecorder();

    private AtomicLong count = new AtomicLong(0);

    private AtomicLong time = new AtomicLong(0);

    private ReqRecorder() {

    }

    public static ReqRecorder getInstance() {
        return container;
    }

    public void addRecord(long time) {
        this.time.addAndGet(time);
        count.incrementAndGet();
    }

    public AtomicLong getCounts() {
        return count;
    }

    public AtomicLong getTime() {
        return time;
    }
}
