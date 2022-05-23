package com.sequoiacm.mappingutil.exec;

import com.google.gson.JsonObject;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.common.module.ScmBucketAttachFailure;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import static com.sequoiacm.mappingutil.common.CommonDefine.MappingStatus.*;

public class MappingProgress {

    private volatile String status = MAPPING;
    private long marker;
    private AtomicLong success = new AtomicLong(0);
    private AtomicLong error = new AtomicLong(0);
    private AtomicLong process = new AtomicLong(0);
    private AtomicLong processError = new AtomicLong(0);

    private Queue<ScmBucketAttachFailure> unAttachableKeys = new ConcurrentLinkedQueue<>();
    private Queue<String> errorKeys = new ConcurrentLinkedQueue<>();

    public MappingProgress() {

    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", this.status);
        jsonObject.addProperty("success", this.success);
        jsonObject.addProperty("error", this.error);
        jsonObject.addProperty("marker", this.marker);
        return jsonObject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getMarker() {
        return marker;
    }

    public void setMarker(long marker) {
        this.marker = marker;
    }

    public Queue<ScmBucketAttachFailure> getUnAttachableKeys() {
        return unAttachableKeys;
    }

    public Queue<String> getErrorKeys() {
        return errorKeys;
    }

    public long getSuccessCount() {
        return success.get();
    }

    public long getErrorCount() {
        return error.get();
    }

    public long getProcessCount() {
        return process.get();
    }

    public long getProcessErrorCount() {
        return processError.get();
    }

    public void success(long successCount) {
        this.success.addAndGet(successCount);
        this.process.addAndGet(successCount);
    }

    public void error(long errorCount) {
        this.error.addAndGet(errorCount);
        this.process.addAndGet(errorCount);
        this.processError.addAndGet(errorCount);
    }

    public void addErrorKeys(List<ScmId> idList) {
        for (ScmId id : idList) {
            this.errorKeys.add(id.get());
        }
    }

    public void addErrorKey(String key) {
        this.errorKeys.add(key);
    }

    public void addUnAttachableKey(ScmBucketAttachFailure failure) {
        this.unAttachableKeys.add(failure);
    }

    public boolean isFinish() {
        return status.equals(FINISH);
    }

    @Override
    public String toString() {
        return "success: " + success.get() + ", error: " + error + ", process: " + process;
    }
}
