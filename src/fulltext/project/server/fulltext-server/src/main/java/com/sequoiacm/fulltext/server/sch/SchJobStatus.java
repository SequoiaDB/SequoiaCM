package com.sequoiacm.fulltext.server.sch;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.common.BsonUtils;

public class SchJobStatus {
    private long estimateCount;
    private long successCount;
    private long errorCount;
    private float speed;

    public SchJobStatus(long estimateCount, long successCount, long errorCount, long speed) {
        this.estimateCount = estimateCount;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.speed = speed;
    }

    public SchJobStatus(BSONObject obj) {
        if (obj == null) {
            return;
        }
        this.estimateCount = BsonUtils.getNumberOrElse(obj, "estimateCount", 0).longValue();
        this.successCount = BsonUtils.getNumberOrElse(obj, "successCount", 0).longValue();
        this.errorCount = BsonUtils.getNumberOrElse(obj, "errorCount", 0).longValue();
        this.speed = BsonUtils.getNumberOrElse(obj, "speed", 0).floatValue();
    }

    public BSONObject toBsonObject() {
        BasicBSONObject ret = new BasicBSONObject();
        ret.put("estimateCount", estimateCount);
        ret.put("successCount", successCount);
        ret.put("errorCount", errorCount);
        ret.put("speed", speed);
        return ret;
    }

    public void setEstimateCount(long estimateCount) {
        this.estimateCount = estimateCount;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public long getEstimateCount() {
        return estimateCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public float getSpeed() {
        return speed;
    }

}
