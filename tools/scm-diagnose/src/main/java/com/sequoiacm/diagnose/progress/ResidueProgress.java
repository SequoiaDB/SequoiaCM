package com.sequoiacm.diagnose.progress;

import java.util.concurrent.atomic.AtomicLong;

public class ResidueProgress extends Progress {
    private AtomicLong residueCount = new AtomicLong(0);

    public void success(boolean isResidue, long successCount) {
        super.success(successCount);
        if (isResidue) {
            residueCount.getAndAdd(successCount);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append("process: ").append(progressCount.get())
                .append(", success: ").append(successCount.get()).append(", fail: ")
                .append(failureCount.get()).append(", residue: ").append(residueCount.get())
                .toString();
    }

    public AtomicLong getResidueCount() {
        return residueCount;
    }
}
