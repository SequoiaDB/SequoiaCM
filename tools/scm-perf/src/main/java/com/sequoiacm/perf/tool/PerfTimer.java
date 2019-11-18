package com.sequoiacm.perf.tool;

public class PerfTimer {
    private long start;
    private long end;

    public void start() {
        this.start = System.currentTimeMillis();
        this.end = this.start;
    }

    public void stop() {
        this.end = System.currentTimeMillis();
    }

    public long duration() {
        return this.end - this.start;
    }
}
