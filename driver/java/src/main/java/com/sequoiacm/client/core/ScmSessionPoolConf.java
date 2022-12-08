package com.sequoiacm.client.core;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scm session pool config
 */
public class ScmSessionPoolConf {

    private final static Logger logger = LoggerFactory.getLogger(ScmSessionPoolConf.class);

    private static final int DEFAULT_MAX_CACHE_SIZE = 100;
    private static final int DEFAULT_KEEP_ALIVE_TIME = 1200;
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 1000;
    private static final long DEFAULT_CHECK_GATEWAY_URLS_INTERVAL = 10 * 1000;
    private static final long DEFAULT_CLEAR_ABNORMAL_SESSION_INTERVAL = 120 * 1000;

    private int maxCacheSize = DEFAULT_MAX_CACHE_SIZE;

    private int keepAliveTime = DEFAULT_KEEP_ALIVE_TIME;

    // double capacity is used by default
    private int maxConnections = -1;

    private int connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;

    private long checkGatewayUrlsInterval = DEFAULT_CHECK_GATEWAY_URLS_INTERVAL;

    private long synGatewayUrlsInterval = 0;

    private long clearAbnormalSessionInterval = DEFAULT_CLEAR_ABNORMAL_SESSION_INTERVAL;

    private String nodeGroup;

    private ScmType.NodeGroupAccessMode groupAccessMode;

    private ScmConfigOption sessionConfig;

    /**
     * Set the max size of the cache session in the pool.
     *
     * @param maxCacheSize
     *            the max size of the cache session in the pool.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     * @since 3.2
     */
    public void setMaxCacheSize(int maxCacheSize) throws ScmInvalidArgumentException {
        checkGreaterThanZero(maxCacheSize, "maxCacheSize");
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * Set the time in seconds for abandoning a session which keep alive time is up.
     * If a session has not be used for a long time(longer than "keepAliveTime"),
     * the pool will not let it come back. The pool will also clean this kind of
     * abnormal session in the pool periodically. It should be greater than 0 but
     * less than scm.session.maxInactiveInterval of auth-server.
     *
     * @param keepAliveTime
     *            session keep alive time (unit: seconds).
     * @throws ScmInvalidArgumentException
     *             if error happens.
     * @since 3.2
     */
    public void setKeepAliveTime(int keepAliveTime) throws ScmInvalidArgumentException {
        checkGreaterThanZero(keepAliveTime, "keepAliveTime");
        this.keepAliveTime = keepAliveTime;
    }

    /**
     * Set the maximum number of connections to send requests using session. It
     * should be set based on the maximum concurrency of the application system. If
     * not set, the default value is capacity * 2.
     *
     * @param maxConnections
     *            the maximum number of connections.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     * @since 3.2
     */
    public void setMaxConnections(int maxConnections) throws ScmInvalidArgumentException {
        checkGreaterThanZero(maxConnections, "maxConnections");
        this.maxConnections = maxConnections;
    }

    /**
     * Set the timeout in milliseconds used when requesting a connection from the
     * session manager.
     * 
     * @param connectionRequestTimeout
     * @throws ScmInvalidArgumentException
     */
    public void setConnectionRequestTimeout(int connectionRequestTimeout)
            throws ScmInvalidArgumentException {
        checkGreaterThanZero(connectionRequestTimeout, "connectionRequestTimeout");
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    /**
     * Set the interval in milliseconds for checking gateway urls. when
     * "checkGatewayUrlsInterval" is less than 1,000 milliseconds, use 1,000
     * milliseconds instead.
     *
     * @param checkGatewayUrlsInterval
     *            the interval of checking gateway urls (unit: milliseconds).
     * @since 3.2
     */
    public void setCheckGatewayUrlsInterval(long checkGatewayUrlsInterval) {
        if (checkGatewayUrlsInterval < 1000) {
            this.checkGatewayUrlsInterval = 1000;
            logger.warn(
                    "invalid checkGatewayUrlsInterval:{}, reset checkGatewayUrlsInterval to 1000ms",
                    checkGatewayUrlsInterval);
        }
        else {
            this.checkGatewayUrlsInterval = checkGatewayUrlsInterval;
        }

    }

    /**
     * Set the interval in milliseconds for updating gateway urls from
     * service-center. When "synGatewayUrlsInterval" is less than or equal to 0, the
     * pool will stop updating gateway's addresses from service-center. when
     * "synGatewayUrlsInterval" is less than 1,000 milliseconds, use 1,000
     * milliseconds instead.
     *
     * @param synGatewayUrlsInterval
     *            the interval of updating gateway urls from service-center (unit:
     *            milliseconds).
     * @since 3.2
     */
    public void setSynGatewayUrlsInterval(long synGatewayUrlsInterval) {
        if (synGatewayUrlsInterval < 1000 && synGatewayUrlsInterval > 0) {
            this.synGatewayUrlsInterval = 1000;
            logger.warn("invalid synGatewayUrlsInterval:{}, reset synGatewayUrlsInterval to 1000ms",
                    synGatewayUrlsInterval);
        }
        else {
            this.synGatewayUrlsInterval = synGatewayUrlsInterval;
        }

    }

    /**
     * Set the interval in milliseconds for cleaning up abnormal sessions int the
     * pool. when "clearAbnormalSessionInterval" is less than 1,000 milliseconds,
     * use 1,000 milliseconds instead.
     *
     * @param clearAbnormalSessionInterval
     *            the interval of cleaning up abnormal sessions int the pool (unit:
     *            milliseconds).
     * @since 3.2
     */
    public void setClearAbnormalSessionInterval(long clearAbnormalSessionInterval) {
        if (clearAbnormalSessionInterval < 1000) {
            this.clearAbnormalSessionInterval = 1000;
            logger.warn(
                    "invalid clearAbnormalSessionInterval:{}, reset clearAbnormalSessionInterval to 1000ms",
                    clearAbnormalSessionInterval);
        }
        else {
            this.clearAbnormalSessionInterval = clearAbnormalSessionInterval;
        }

    }

    /**
     * Set the session config.
     *
     * @param sessionConfig
     *            the session config.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     * @since 3.2
     */
    public void setSessionConfig(ScmConfigOption sessionConfig) throws ScmInvalidArgumentException {
        checkNotNull(sessionConfig, "sessionConfig");
        this.sessionConfig = sessionConfig;
    }

    /**
     * Get the capacity of the pool.
     * 
     * @return the capacity of the pool
     * @since 3.2
     */
    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    /**
     * Get the setup time for abandoning a session which has not been used for long
     * time.
     *
     * @return the keep alive time
     * @since 3.2
     */
    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    /**
     * Get the max number of connections.
     *
     * @return the max number of connections
     * @since 3.2
     */
    public int getMaxConnections() {
        if (maxConnections == -1) {
            return maxCacheSize * 2;
        }
        return maxConnections;
    }

    /**
     * Get the timeout in milliseconds used when requesting a connection from the
     * session manager.
     * 
     * @return the connection request timeout
     * @since 3.2
     */
    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    /**
     * Get the interval for checking gateway urls.
     *
     * @return the interval
     * @since 3.2
     */
    public long getCheckGatewayUrlsInterval() {
        return checkGatewayUrlsInterval;
    }

    /**
     * Get the interval for updating gateway urls from service-center.
     *
     * @return the interval
     * @since 3.2
     */
    public long getSynGatewayUrlsInterval() {
        return synGatewayUrlsInterval;
    }

    /**
     * Get the interval for cleaning up abnormal sessions.
     *
     * @return the interval
     * @since 3.2
     */
    public long getClearAbnormalSessionInterval() {
        return clearAbnormalSessionInterval;
    }

    /**
     * Get the session config.
     *
     * @return the session config
     * @since 3.2
     */
    public ScmConfigOption getSessionConfig() {
        return sessionConfig;
    }

    /**
     * Get the node group.
     * 
     * @since 3.2.2
     * @return the node group.
     */
    public String getNodeGroup() {
        return nodeGroup;
    }

    /**
     * Set the node group, It will control the use of the gateway address when
     * creating the session.
     * 
     * @param nodeGroup
     *            the node group.
     * @since 3.2.2
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setNodeGroup(String nodeGroup) throws ScmInvalidArgumentException {
        checkNotNull(nodeGroup, "nodeGroup");
        this.nodeGroup = nodeGroup;
        if (this.groupAccessMode == null) {
            this.groupAccessMode = ScmType.NodeGroupAccessMode.getDefaultAccessMode(nodeGroup);
        }
    }

    /**
     * Get the group access mode.
     * 
     * @since 3.2.2
     * @return the group access mode.
     */
    public ScmType.NodeGroupAccessMode getGroupAccessMode() {
        return groupAccessMode;
    }

    /**
     * Set the group access mode.
     * 
     * @see ScmType.NodeGroupAccessMode
     * @param groupAccessMode
     *            the group access mode.
     * @throws ScmInvalidArgumentException
     *             if error happens.
     */
    public void setGroupAccessMode(ScmType.NodeGroupAccessMode groupAccessMode)
            throws ScmInvalidArgumentException {
        checkNotNull(groupAccessMode, "groupAccessMode");
        this.groupAccessMode = groupAccessMode;
    }

    /**
     * Create a config builder.
     *
     * @return builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    private void checkNotNull(Object value, String name) throws ScmInvalidArgumentException {
        if (value == null) {
            throw new ScmInvalidArgumentException(name + " cannot be null");
        }
    }

    private void checkGreaterThanZero(int value, String name) throws ScmInvalidArgumentException {
        if (value <= 0) {
            throw new ScmInvalidArgumentException(
                    name + " must be greater than 0, " + name + ":" + value);
        }
    }

    public static class Builder {

        private final ScmSessionPoolConf conf;

        public Builder() {
            conf = new ScmSessionPoolConf();
        }

        public Builder setMaxCacheSize(int maxCacheSize) throws ScmInvalidArgumentException {
            conf.setMaxCacheSize(maxCacheSize);
            return this;
        }

        public Builder setKeepAliveTime(int keepAliveTime) throws ScmInvalidArgumentException {
            conf.setKeepAliveTime(keepAliveTime);
            return this;
        }

        public Builder setMaxConnections(int maxConnections) throws ScmInvalidArgumentException {
            conf.setMaxConnections(maxConnections);
            return this;
        }

        public Builder setConnectionRequestTimeout(int connectionRequestTimeout)
                throws ScmInvalidArgumentException {
            conf.setConnectionRequestTimeout(connectionRequestTimeout);
            return this;
        }

        public Builder setCheckGatewayUrlsInterval(int checkGatewayUrlsInterval)
                throws ScmInvalidArgumentException {
            conf.setCheckGatewayUrlsInterval(checkGatewayUrlsInterval);
            return this;
        }

        public Builder setSynGatewayUrlsInterval(int synGatewayUrlsInterval)
                throws ScmInvalidArgumentException {
            conf.setSynGatewayUrlsInterval(synGatewayUrlsInterval);
            return this;
        }

        public Builder setClearAbnormalSessionInterval(int clearAbnormalSessionInterval)
                throws ScmInvalidArgumentException {
            conf.setClearAbnormalSessionInterval(clearAbnormalSessionInterval);
            return this;
        }

        public Builder setSessionConfig(ScmConfigOption sessionConfig)
                throws ScmInvalidArgumentException {
            conf.setSessionConfig(sessionConfig);
            return this;
        }

        public ScmSessionPoolConf get() throws ScmInvalidArgumentException {
            return conf;
        }

        public Builder setNodeGroup(String group) throws ScmInvalidArgumentException {
            conf.setNodeGroup(group);
            return this;
        }

        public Builder setGroupAccessMode(ScmType.NodeGroupAccessMode groupAccessMode)
                throws ScmInvalidArgumentException {
            conf.setGroupAccessMode(groupAccessMode);
            return this;
        }
    }

}
