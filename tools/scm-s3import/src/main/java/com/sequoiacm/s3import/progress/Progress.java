package com.sequoiacm.s3import.progress;

import com.google.gson.JsonObject;
import com.sequoiacm.s3import.common.CommonDefine;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Progress {

    protected String status = CommonDefine.ProgressStatus.INIT;
    protected long totalCount;
    protected AtomicLong successCount = new AtomicLong(0);
    protected AtomicLong failureCount = new AtomicLong(0);
    protected AtomicLong processCount = new AtomicLong(0);

    public static final String STATUS = "status";
    public static final String TOTAL_COUNT = "total_count";

    public Progress() {

    }

    public void init(JsonObject progress) {
        this.status = progress.get(STATUS).getAsString();
        this.totalCount = progress.get(TOTAL_COUNT).getAsLong();
    }

    public JsonObject toProgressJson() {
        JsonObject progress = new JsonObject();
        progress.addProperty(STATUS, this.status);
        progress.addProperty(TOTAL_COUNT, getTotalCount());
        return progress;
    }

    public void success() {
        successCount.incrementAndGet();
        processCount.incrementAndGet();
    }

    public void success(String type) {
        successCount.incrementAndGet();
        processCount.incrementAndGet();
    }

    public void failed() {
        failureCount.incrementAndGet();
        processCount.incrementAndGet();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public long getSuccessCount() {
        return successCount.get();
    }

    public AtomicLong getFailureCount() {
        return failureCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

}
