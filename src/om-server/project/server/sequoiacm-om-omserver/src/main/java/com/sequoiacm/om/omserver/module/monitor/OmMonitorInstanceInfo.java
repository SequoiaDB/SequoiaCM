package com.sequoiacm.om.omserver.module.monitor;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmMonitorInstanceInfo extends OmMonitorInstanceBasicInfo {

    @JsonProperty("instance_id")
    private String instanceId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("management_url")
    private String managementUrl;

    public OmMonitorInstanceInfo(String instanceId, String status, String managementUrl,
            OmMonitorInstanceBasicInfo basicInfo) {
        this.setInstanceId(instanceId);
        this.setStatus(status);
        this.setManagementUrl(managementUrl);
        this.setHostName(basicInfo.getHostName());
        this.setIpAddr(basicInfo.getIpAddr());
        this.setMetadata(basicInfo.getMetadata());
        this.setPort(basicInfo.getPort());
        this.setRegion(basicInfo.getRegion());
        this.setZone(basicInfo.getZone());
        this.setManagementPort(basicInfo.getManagementPort());
        this.setHealthCheckUrl(basicInfo.getHealthCheckUrl());
        this.setServiceName(basicInfo.getServiceName());
        this.setManualStopped(basicInfo.isManualStopped());
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getManagementUrl() {
        return managementUrl;
    }

    public void setManagementUrl(String managementUrl) {
        this.managementUrl = managementUrl;
    }

}
