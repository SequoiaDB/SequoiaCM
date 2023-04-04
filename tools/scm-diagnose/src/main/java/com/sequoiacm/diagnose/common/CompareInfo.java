package com.sequoiacm.diagnose.common;

import com.sequoiacm.diagnose.progress.CompareProgress;

public class CompareInfo {
    private String workspace;
    private String beginTime;
    private String endTime;
    private CompareProgress progress;
    private CheckLevel checkLevel;
    private boolean isFull;

    public CompareInfo(String workspace, String beginTime, String endTime, CompareProgress progress,
            CheckLevel checkLevel, boolean isFull) {
        this.workspace = workspace;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.progress = progress;
        this.checkLevel = checkLevel;
        this.isFull = isFull;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getBeginTime() {
        return beginTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public CompareProgress getProgress() {
        return progress;
    }

    public CheckLevel getCheckLevel() {
        return checkLevel;
    }

    public boolean isFull() {
        return isFull;
    }
}
