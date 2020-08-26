package com.sequoiacm.infrastructure.fulltext.core;

import org.bson.BSONObject;

import com.sequoiacm.infrastructure.common.BsonUtils;

/**
 * Fulltext job information.
 */
public class ScmFulltextJobInfo {
    public static final String KEY_ESTIMATE_FILE_COUNT = "estimateFileCount";
    public static final String KEY_PROGRESS = "progress";
    public static final String KEY_SPEED = "speed";
    public static final String KEY_SUCCESS_COUNT = "successCount";
    public static final String KEY_ERROR_COUNT = "errorCount";
    private long estimateFileCount;
    private int progress;
    private float speed;
    private long successCount;
    private long errorCount;

    public ScmFulltextJobInfo() {
    }

    public ScmFulltextJobInfo(BSONObject obj) {
        if (obj == null) {
            return;
        }
        estimateFileCount = BsonUtils.getNumberOrElse(obj, KEY_ESTIMATE_FILE_COUNT, 0).longValue();
        errorCount = BsonUtils.getNumberOrElse(obj, KEY_ERROR_COUNT, 0).longValue();
        successCount = BsonUtils.getNumberOrElse(obj, KEY_SUCCESS_COUNT, 0).longValue();
        progress = BsonUtils.getNumberOrElse(obj, KEY_PROGRESS, 0).intValue();
        speed = BsonUtils.getNumberOrElse(obj, KEY_SPEED, 0).floatValue();
    }

    /**
     * Get the estimate file count.
     * 
     * @return file count.
     */
    public long getEstimateFileCount() {
        return estimateFileCount;
    }

    public void setEstimateFileCount(long estimateFileCount) {
        this.estimateFileCount = estimateFileCount;
    }

    /**
     * Get the progress.
     * 
     * @return progress.
     */
    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    /**
     * Get the speed, files per second.
     * 
     * @return speed.
     */
    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * Get the error count.
     * 
     * @return error count.
     */
    public long getErrorCount() {
        return errorCount;
    }

    /**
     * Get the success count.
     * 
     * @return success count.
     */
    public long getSuccessCount() {
        return successCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    @Override
    public String toString() {
        return "ScmWorkspaceFulltextJobInfo [estimateFileCount=" + estimateFileCount + ", progress="
                + progress + ", speed=" + speed + ", successCount=" + successCount + ", errorCount="
                + errorCount + "]";
    }

}
