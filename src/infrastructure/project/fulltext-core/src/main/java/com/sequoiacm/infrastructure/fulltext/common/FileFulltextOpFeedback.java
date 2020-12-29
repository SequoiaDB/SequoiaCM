package com.sequoiacm.infrastructure.fulltext.common;

import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;

public class FileFulltextOpFeedback {
    public static final String KEY_SUCCESS_COUNT = "success_count";
    public static final String KEY_FAILED_COUNT = "failed_count";
    private int successCount = 0;
    private int failedCount = 0;

    public FileFulltextOpFeedback(BSONObject obj) {
        this.successCount = BsonUtils.getInteger(obj, FileFulltextOpFeedback.KEY_SUCCESS_COUNT);
        this.failedCount = BsonUtils.getInteger(obj, FileFulltextOpFeedback.KEY_FAILED_COUNT);
    }

    public FileFulltextOpFeedback(int successCount, int failedCount) {
        this.successCount = successCount;
        this.failedCount = failedCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    @Override public String toString() {
        return "FileFulltextOpFeedback{" + "successCount=" + successCount + ", failedCount="
                + failedCount + '}';
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }
}
