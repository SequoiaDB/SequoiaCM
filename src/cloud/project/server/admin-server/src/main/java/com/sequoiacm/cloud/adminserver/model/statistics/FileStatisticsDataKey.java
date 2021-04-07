package com.sequoiacm.cloud.adminserver.model.statistics;

import java.util.Objects;

public class FileStatisticsDataKey {
    private String time;
    private String user;
    private String workspace;
    private String type;

    public FileStatisticsDataKey(String time, String user, String workspace, String type) {
        this.time = time;
        this.user = user;
        this.workspace = workspace;
        this.type = type;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "FileStatisticsDataKey{" + "time='" + time + '\'' + ", user='" + user + '\''
                + ", workspace='" + workspace + '\'' + ", type='" + type + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FileStatisticsDataKey that = (FileStatisticsDataKey) o;
        return Objects.equals(time, that.time) && Objects.equals(user, that.user)
                && Objects.equals(workspace, that.workspace) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, user, workspace, type);
    }
}
