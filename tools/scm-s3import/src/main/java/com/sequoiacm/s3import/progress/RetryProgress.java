package com.sequoiacm.s3import.progress;

public class RetryProgress extends Progress {

    @Override
    public String toString() {
        return new StringBuilder()
                .append("total:").append(totalCount)
                .append(", success:").append(successCount)
                .append(", fail:").append(failureCount)
                .toString();
    }
}
