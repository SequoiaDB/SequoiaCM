package com.sequoiacm.infrastructure.common;

public class ScmParesedId {
    private long seconds;
    private int clusterId;
    private int serverId;
    private int serial;
    private int threadId;
    private int timezoneId;
    private String id;

    ScmParesedId(String id, long seconds, int clusterId, int serverId, int threadId, int serial,
            int timezoneId) {
        this.seconds = seconds;
        this.clusterId = clusterId;
        this.serverId = serverId;
        this.serial = serial;
        this.threadId = threadId;
        this.timezoneId = timezoneId;
        this.id = id;
    }

    long getSeconds() {
        return seconds;
    }

    int getThreadId() {
        return threadId;
    }

    int getClusterId() {
        return clusterId;
    }

    int getServerId() {
        return serverId;
    }

    int getSerial() {
        return serial;
    }

    String getId() {
        return id;
    }

    int getTimezoneId() {
        return timezoneId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("id:").append(id).append(", ");
        sb.append("seconds:").append(seconds).append(", ");
        sb.append("clusterId:").append(clusterId).append(", ");
        sb.append("serverId:").append(serverId).append(", ");
        sb.append("threadId:").append(threadId).append(", ");
        sb.append("inc: ").append(serial);
        sb.append("timezoneId: ").append(timezoneId);
        sb.append("}");

        return sb.toString();
    }
}
