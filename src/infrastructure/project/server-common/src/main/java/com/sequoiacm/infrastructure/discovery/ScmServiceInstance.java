package com.sequoiacm.infrastructure.discovery;

import java.util.Map;
import java.util.Objects;

public class ScmServiceInstance {
    private String host;
    private int port;
    private String region;
    private String zone;
    private Map<String, String> metadata;
    private String serviceName;

    public ScmServiceInstance(String host, int port, String region, String zone,
            Map<String, String> metadata, String serviceName) {
        this.host = host;
        this.port = port;
        this.region = region;
        this.zone = zone;
        this.metadata = metadata;
        this.serviceName = serviceName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getRegion() {
        return region;
    }

    public String getZone() {
        return zone;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String toString() {
        return "ScmServiceInstance{" + "host='" + host + '\'' + ", port=" + port + ", region='"
                + region + '\'' + ", zone='" + zone + '\'' + ", metadata=" + metadata
                + ", serviceName='" + serviceName + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScmServiceInstance that = (ScmServiceInstance) o;
        return port == that.port && Objects.equals(host, that.host)
                && Objects.equals(region, that.region) && Objects.equals(zone, that.zone)
                && Objects.equals(metadata, that.metadata)
                && Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, region, zone, metadata, serviceName);
    }
}
