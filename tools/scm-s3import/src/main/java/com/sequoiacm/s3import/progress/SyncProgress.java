package com.sequoiacm.s3import.progress;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.sequoiacm.s3import.common.CommonDefine.SyncType.*;

public class SyncProgress extends Progress {

    private Map<String, AtomicLong> operationCounter = new HashMap<>();

    public SyncProgress() {
        operationCounter.put(ADD, new AtomicLong(0));
        operationCounter.put(DELETE, new AtomicLong(0));
        operationCounter.put(UPDATE, new AtomicLong(0));
    }

    @Override
    public void success(String syncType) {
        super.success(syncType);
        AtomicLong operationCount = operationCounter.get(syncType);
        operationCount.incrementAndGet();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("add:").append(this.operationCounter.get(ADD)).append(", delete:")
                .append(this.operationCounter.get(DELETE)).append(", update:")
                .append(this.operationCounter.get(UPDATE))
                .append(", fail:").append(this.failureCount)
                .toString();
    }
}
