package com.sequoiacm.om.omserver.module.monitor;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

public class OmMonitorInstanceBasicInfo {

    @JsonProperty("ip_addr")
    private String ipAddr;

    @JsonProperty("port")
    private int port;

    @JsonProperty("management_port")
    private int managementPort;

    @JsonProperty("service_name")
    private String serviceName;

    @JsonProperty("host_name")
    private String hostName;

    @JsonProperty("region")
    private String region;

    @JsonProperty("zone")
    private String zone;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("is_manual_stopped")
    private boolean isManualStopped;

    @JsonProperty("health_check_url")
    private String healthCheckUrl;

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getManagementPort() {
        return managementPort;
    }

    public void setManagementPort(int managementPort) {
        this.managementPort = managementPort;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public boolean isManualStopped() {
        return isManualStopped;
    }

    public void setManualStopped(boolean manualStopped) {
        isManualStopped = manualStopped;
    }

    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    public void setHealthCheckUrl(String healthCheckUrl) {
        this.healthCheckUrl = healthCheckUrl;
    }

    @Override
    public String toString() {
        return "ScmInstanceInfo{" + "ipAddr='" + ipAddr + '\'' + ", port=" + port
                + ", managementPort=" + managementPort + ", serviceName='" + serviceName + '\''
                + ", hostName='" + hostName + '\'' + ", region='" + region + '\'' + ", zone='"
                + zone + '\'' + ", metadata=" + metadata + ", is_manual_stopped=" + isManualStopped
                + ", healthCheckUrl='" + healthCheckUrl + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OmMonitorInstanceBasicInfo that = (OmMonitorInstanceBasicInfo) o;
        return port == that.port && Objects.equals(ipAddr, that.ipAddr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddr, port);
    }
}
