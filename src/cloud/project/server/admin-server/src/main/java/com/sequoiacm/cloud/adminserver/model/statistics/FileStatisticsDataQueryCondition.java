package com.sequoiacm.cloud.adminserver.model.statistics;

import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;

public class FileStatisticsDataQueryCondition {
    private String user;
    private String workspace;
    // yyyy-MM-dd HH:mm:ss
    private String begin;
    // yyyy-MM-dd HH:mm:ss
    private String end;
    private ScmTimeAccuracy time_accuracy;

    public FileStatisticsDataQueryCondition() {
    }

    public FileStatisticsDataQueryCondition(String user, String workspace, String begin, String end,
            ScmTimeAccuracy timeAccuracy) {
        this.user = user;
        this.workspace = workspace;
        this.begin = begin;
        this.end = end;
        this.time_accuracy = timeAccuracy;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getBegin() {
        return begin;
    }

    public void setBegin(String begin) {
        this.begin = begin;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public ScmTimeAccuracy getTimeAccuracy() {
        return time_accuracy;
    }

    public void setTimeAccuracy(ScmTimeAccuracy timeAccuracy) {
        this.time_accuracy = timeAccuracy;
    }

    @Override
    public String toString() {
        return "FileStatisticsDataQueryCondition{" + "user='" + user + '\'' + ", workspace='" + workspace
                + '\'' + ", begin='" + begin + '\'' + ", end='" + end + '\'' + ", timeAccuracy="
                + time_accuracy + '}';
    }
}
