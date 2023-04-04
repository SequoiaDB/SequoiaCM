package com.sequoiacm.diagnose.progress;

import java.util.concurrent.atomic.AtomicLong;

public class CompareProgress extends Progress {
    private AtomicLong sameCount = new AtomicLong(0);
    private AtomicLong diffCount = new AtomicLong(0);

    public void success(boolean isSame, long count) {
        super.success(count);
        if (isSame) {
            sameCount.getAndAdd(count);
        }
        else {
            diffCount.getAndAdd(count);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append("process: ").append(progressCount).append(", fail: ")
                .append(failureCount).append(", same: ").append(sameCount).append(", different: ")
                .append(diffCount).toString();
    }
}
