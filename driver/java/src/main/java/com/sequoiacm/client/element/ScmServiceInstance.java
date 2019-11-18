package com.sequoiacm.client.element;

/**
 * Scm service instance.
 */
public class ScmServiceInstance {
    private int port;
    private String serviceName;
    private String ip;
    private String region;
    private String zone;
    private String status;
    private boolean isContentServer;
    private boolean isRootSite;

    /**
     * Create scm service instance with specified args.
     *
     * @param serviceName
     *            service name.
     * @param region
     *            region name.
     * @param zone
     *            zone name.
     * @param ip
     *            ip.
     * @param port
     *            port.
     * @param status
     *            status of service instance.
     */
    public ScmServiceInstance(String serviceName, String region, String zone, String ip, int port,
            String status) {
        this(serviceName, region, zone, ip, port, status, false, false);
    }

    /**
     * Create scm service instance with specified args.
     *
     * @param serviceName
     *            service name.
     * @param region
     *            region name.
     * @param zone
     *            zone name.
     * @param ip
     *            ip.
     * @param port
     *            port.
     * @param status
     *            status of service instance.
     * @param isContentServer
     *            if the service instance is content server.
     *
     * @param isRootSite
     *            if the service is root site
     */
    public ScmServiceInstance(String serviceName, String region, String zone, String ip, int port,
            String status, boolean isContentServer, boolean isRootSite) {
        super();
        this.port = port;
        this.serviceName = serviceName;
        this.ip = ip;
        this.zone = zone;
        this.region = region;
        this.status = status;
        this.isContentServer = isContentServer;
        this.isRootSite = isRootSite;
    }

    /**
     * Returns the region of the service instance.
     *
     * @return region.
     */
    public String getRegion() {
        return region;
    }

    /**
     * Returns the zone of the service instance.
     *
     * @return zone.
     */
    public String getZone() {
        return zone;
    }

    /**
     * Returns the ip of the service instance.
     *
     * @return ip.
     */
    public String getIp() {
        return ip;
    }

    /**
     * Returns the port of the service instance.
     *
     * @return port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the name of the service instance.
     *
     * @return service name.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the status of the service instance.
     *
     * @return status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Returns true if the service instance is content server.
     *
     * @return true or false.
     */
    public boolean isContentServer() {
        return isContentServer;
    }

    /**
     * Returns true if the service instance is content server and belong to an
     * root site.
     *
     * @return true or false.
     */
    public boolean isRootSite() {
        return isRootSite;
    }

    @Override
    public String toString() {
        return "ScmServerInstance [port=" + port + ", serviceName=" + serviceName + ", ip=" + ip
                + ", region=" + region + ", zone=" + zone + "]";
    }

}
