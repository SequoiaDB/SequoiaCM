package com.sequoiacm.infrastructure.common;

public class ScmParesedId {
    private long seconds;
    private int clusterId;
    private int serverId;
    private int serial;
    private int threadId;

    ScmParesedId(long seconds, int clusterId, int serverId, int threadId, int serial) {
        this.seconds = seconds;
        this.clusterId = clusterId;
        this.serverId = serverId;
        this.serial = serial;
        this.threadId = threadId;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("seconds:").append(seconds).append(", ");
        sb.append("clusterId:").append(clusterId).append(", ");
        sb.append("serverId:").append(serverId).append(", ");
        sb.append("threadId:").append(threadId).append(", ");
        sb.append("inc: ").append(serial);
        sb.append("}");

        return sb.toString();
    }
}
