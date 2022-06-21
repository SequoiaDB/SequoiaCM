package com.sequoiacm.s3import.progress;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sequoiacm.s3import.common.CommonDefine;
import static com.sequoiacm.s3import.common.CommonDefine.DiffType.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class CompareProgress extends Progress {

    public static final String SRC_NEXT_KEY_MARKER = "src_next_key_marker";
    public static final String DEST_NEXT_KEY_MARKER = "dest_next_key_marker";

    private String srcNextKeyMarker = CommonDefine.KeyMarker.BEGINNING;
    private String destNextKeyMarker = CommonDefine.KeyMarker.BEGINNING;
    private Map<String, AtomicLong> diffCounter = new HashMap<>();

    public CompareProgress() {
        diffCounter.put(SAME, new AtomicLong(0));
        diffCounter.put(NEW, new AtomicLong(0));
        diffCounter.put(DELETED, new AtomicLong(0));
        diffCounter.put(DIFF, new AtomicLong(0));
    }

    @Override
    public void init(JsonObject progress) {
        super.init(progress);
        this.diffCounter.put(SAME, new AtomicLong(progress.get(SAME).getAsLong()));
        this.diffCounter.put(NEW, new AtomicLong(progress.get(NEW).getAsLong()));
        this.diffCounter.put(DELETED, new AtomicLong(progress.get(DELETED).getAsLong()));
        this.diffCounter.put(DIFF, new AtomicLong(progress.get(DIFF).getAsLong()));

        JsonElement srcNextMarker = progress.get(SRC_NEXT_KEY_MARKER);
        JsonElement destNextMarker = progress.get(DEST_NEXT_KEY_MARKER);
        this.srcNextKeyMarker = srcNextMarker.toString().equals("null") ? null :srcNextMarker.getAsString();
        this.destNextKeyMarker = destNextMarker.toString().equals("null") ? null :destNextMarker.getAsString();
    }

    @Override
    public JsonObject toProgressJson() {
        JsonObject progress = super.toProgressJson();
        progress.addProperty(SAME, this.diffCounter.get(SAME));
        progress.addProperty(NEW, this.diffCounter.get(NEW));
        progress.addProperty(DELETED, this.diffCounter.get(DELETED));
        progress.addProperty(DIFF, this.diffCounter.get(DIFF));
        progress.addProperty(SRC_NEXT_KEY_MARKER, this.srcNextKeyMarker);
        progress.addProperty(DEST_NEXT_KEY_MARKER, this.destNextKeyMarker);
        return progress;
    }

    @Override
    public void success(String diffType) {
        super.success(diffType);
        AtomicLong diffCount = diffCounter.get(diffType.startsWith(DIFF) ? DIFF : diffType);
        diffCount.incrementAndGet();
    }

    @Override
    public long getTotalCount() {
        return diffCounter.get(SAME).get() + diffCounter.get(NEW).get()
                + diffCounter.get(DELETED).get() + diffCounter.get(DIFF).get();
    }

    public String getSrcNextKeyMarker() {
        return srcNextKeyMarker;
    }

    public void setSrcNextKeyMarker(String srcNextKeyMarker) {
        this.srcNextKeyMarker = srcNextKeyMarker;
    }

    public String getDestNextKeyMarker() {
        return destNextKeyMarker;
    }

    public void setDestNextKeyMarker(String destNextKeyMarker) {
        this.destNextKeyMarker = destNextKeyMarker;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("same:").append(diffCounter.get(SAME)).append(", new:")
                .append(diffCounter.get(NEW)).append(", deleted:").append(diffCounter.get(DELETED))
                .append(", diff:").append(diffCounter.get(DIFF))
                .toString();
    }
}
