package com.sequoiacm.s3import.progress;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sequoiacm.s3import.common.CommonDefine;

import java.util.concurrent.atomic.AtomicLong;

public class MigrateProgress extends Progress {

    public static final String NEXT_KEY_MARKER = "next_key_marker";
    public static final String SUCCESS_COUNT = "success_count";
    public static final String FAIL_COUNT = "fail_count";

    private String nextKeyMarker = CommonDefine.KeyMarker.BEGINNING;

    public MigrateProgress() {

    }

    @Override
    public void init(JsonObject migrateProgress) {
        super.init(migrateProgress);
        this.successCount = new AtomicLong(migrateProgress.get(SUCCESS_COUNT).getAsLong());
        this.failureCount = new AtomicLong(migrateProgress.get(FAIL_COUNT).getAsLong());
        JsonElement nextKeyMarker = migrateProgress.get(NEXT_KEY_MARKER);
        this.nextKeyMarker = nextKeyMarker.toString().equals("null") ? null : nextKeyMarker.getAsString();
    }

    @Override
    public JsonObject toProgressJson() {
        JsonObject progress = super.toProgressJson();
        progress.addProperty(MigrateProgress.SUCCESS_COUNT, this.successCount);
        progress.addProperty(MigrateProgress.FAIL_COUNT, this.failureCount);
        progress.addProperty(MigrateProgress.NEXT_KEY_MARKER, this.nextKeyMarker);
        return progress;
    }

    @Override
    public long getTotalCount() {
        return this.successCount.get() + this.failureCount.get();
    }

    public void setNextKeyMarker(String nextKeyMarker) {
        this.nextKeyMarker = nextKeyMarker;
    }

    public String getNextKeyMarker() {
        return nextKeyMarker;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("success:").append(successCount).append(", fail:")
                .append(failureCount).append(", process:").append(processCount).toString();
    }
}
