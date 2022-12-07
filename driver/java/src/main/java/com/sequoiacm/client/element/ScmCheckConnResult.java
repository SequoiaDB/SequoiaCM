package com.sequoiacm.client.element;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import org.bson.BSONObject;

public class ScmCheckConnResult {
    private boolean connected;
    private String service;
    private String host;
    private Integer port;
    private String region;
    private String zone;
    private String ip;

    /**
     * Create an instance of ScmCheckConnResult.
     *
     * @param obj
     *            a bson containing basic information about scm check connectivity result.
     *
     */
    public ScmCheckConnResult(BSONObject obj) throws ScmException {
        service = BsonUtils.getStringChecked(obj, "service");
        ip = BsonUtils.getStringChecked(obj, "ip");
        connected = BsonUtils.getBoolean(obj, "connected");
        host = BsonUtils.getStringChecked(obj, "host");
        port = BsonUtils.getIntegerChecked(obj, "port");
        region = BsonUtils.getStringChecked(obj, "region");
        zone = BsonUtils.getStringChecked(obj, "zone");
    }

    /**
     * Return the ScmCheckConnResult is connected or not
     *
     * @return return true if is connected.
     * @since 3.2.2
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Return the value of service name property
     *
     * @return service name
     * @since 3.2.2
     */
    public String getService() {
        return service;
    }

    /**
     * Return the value of host name property
     *
     * @return host name
     * @since 3.2.2
     */
    public String getHost() {
        return host;
    }

    /**
     * Return the value of port property
     *
     * @return port
     * @since 3.2.2
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Return the value of region property
     *
     * @return region
     * @since 3.2.2
     */
    public String getRegion() {
        return region;
    }

    /**
     * Return the value of zone property
     *
     * @return zone
     * @since 3.2.2
     */
    public String getZone() {
        return zone;
    }

    /**
     * Return the value of ip property
     *
     * @return ip
     * @since 3.2.2
     */
    public String getIp() {
        return ip;
    }

}
