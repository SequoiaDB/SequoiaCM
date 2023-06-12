package com.sequoiacm.infrastructure.config.core.customizer;

public class VersionHeartbeatOption {
    public static final VersionHeartbeatOption DEFAULT = new VersionHeartbeatOption();

    private boolean globalVersionHeartbeat;
    private String globalVersionName;

    private int initStatusHeartbeatInterval = -1;

    public VersionHeartbeatOption() {
    }

    public boolean isGlobalVersionHeartbeat() {
        return globalVersionHeartbeat;
    }

    public void setGlobalVersionHeartbeat(boolean globalVersionHeartbeat) {
        this.globalVersionHeartbeat = globalVersionHeartbeat;
    }

    public String getGlobalVersionName() {
        return globalVersionName;
    }

    public void setGlobalVersionName(String globalVersionName) {
        this.globalVersionName = globalVersionName;
    }

    public int getInitStatusHeartbeatInterval() {
        return initStatusHeartbeatInterval;
    }

    public void setInitStatusHeartbeatInterval(int initStatusHeartbeatInterval) {
        this.initStatusHeartbeatInterval = initStatusHeartbeatInterval;
    }

    @Override
    public String toString() {
        return "VersionHeartbeatOption{" + "globalVersionHeartbeat=" + globalVersionHeartbeat
                + ", globalVersionName='" + globalVersionName + '\''
                + ", initStatusHeartbeatInterval=" + initStatusHeartbeatInterval + '}';
    }
}
