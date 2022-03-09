package com.sequoiacm.om.omserver.common;

public enum InstanceStatus {

    UP("UP"),
    DOWN("DOWN"),
    STOPPED("STOPPED");

    private String statusText;

    InstanceStatus(String statusText) {
        this.statusText = statusText;
    }

    public String getStatusText() {
        return statusText;
    }

    public InstanceStatus getType(String status) {
        for (InstanceStatus value : InstanceStatus.values()) {
            if (value.statusText.equalsIgnoreCase(status)) {
                return value;
            }
        }
        return null;
    }

}
