package com.sequoiacm.cloud.servicecenter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.cloud.servicecenter.common.FieldDefine;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;

import java.util.Objects;

public class ScmInstance {

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
    private BSONObject metadata;

    @JsonProperty("is_manual_stopped")
    private boolean isManualStopped;

    @JsonProperty("health_check_url")
    private String healthCheckUrl;

    public ScmInstance() {
    }

    public ScmInstance(BSONObject bson) {
        this.ipAddr = BsonUtils.getStringChecked(bson, FieldDefine.Instance.FIELD_IP_ADDR);
        this.port = BsonUtils.getIntegerChecked(bson, FieldDefine.Instance.FIELD_PORT);
        this.managementPort = BsonUtils.getIntegerChecked(bson,
                FieldDefine.Instance.FIELD_MANAGEMENT_PORT);
        this.serviceName = BsonUtils.getStringChecked(bson,
                FieldDefine.Instance.FIELD_SERVICE_NAME);
        this.hostName = BsonUtils.getStringChecked(bson, FieldDefine.Instance.FIELD_HOST_NAME);
        this.region = BsonUtils.getStringChecked(bson, FieldDefine.Instance.FIELD_REGION);
        this.zone = BsonUtils.getStringChecked(bson, FieldDefine.Instance.FIELD_ZONE);
        this.metadata = BsonUtils.getBSONChecked(bson, FieldDefine.Instance.FIELD_METADATA);
        this.isManualStopped = BsonUtils.getBooleanChecked(bson,
                FieldDefine.Instance.FIELD_IS_MANUAL_STOPPED);
        this.healthCheckUrl = BsonUtils.getStringChecked(bson,
                FieldDefine.Instance.FIELD_HEALTH_CHECK_URL);
    }

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

    public BSONObject getMetadata() {
        return metadata;
    }

    public void setMetadata(BSONObject metadata) {
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
                + zone + '\'' + ", metadata=" + metadata + ", isManualStopped=" + isManualStopped
                + ", healthCheckUrl='" + healthCheckUrl + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScmInstance that = (ScmInstance) o;
        return port == that.port && managementPort == that.managementPort
                && isManualStopped == that.isManualStopped && Objects.equals(ipAddr, that.ipAddr)
                && Objects.equals(serviceName, that.serviceName)
                && Objects.equals(hostName, that.hostName) && Objects.equals(region, that.region)
                && Objects.equals(zone, that.zone) && Objects.equals(metadata, that.metadata)
                && Objects.equals(healthCheckUrl, that.healthCheckUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddr, port, managementPort, serviceName, hostName, region, zone,
                metadata, isManualStopped, healthCheckUrl);
    }
}
