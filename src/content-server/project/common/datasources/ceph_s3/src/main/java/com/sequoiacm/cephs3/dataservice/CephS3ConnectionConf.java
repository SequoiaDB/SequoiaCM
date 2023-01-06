package com.sequoiacm.cephs3.dataservice;

import java.util.Map;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.retry.PredefinedRetryPolicies;

public class CephS3ConnectionConf {
    static final String DEFAULT_SIGNER_OVERRIDE = "S3SignerType";
    static final String CONF_KEY_SIGNER_OVERRIDE = "signerOverride";
    static final String CONF_KEY_SOCKET_TIMEOUT = "socketTimeout";
    static final String CONF_KEY_MAX_CONNS = "maxConns";
    static final String CONF_KEY_CONN_TTL = "connTTL";
    static final String CONF_KEY_CONN_TIMEOUT = "connTimeout";
    static final String CONF_KEY_MAX_ERROR_RETRY = "maxErrorRetry";
    static final String CONF_KEY_USE_EXPECT_CONTINUE = "useExpectContinue";

    static final String CONF_KEY_IDLE_CLIENT_CLEAN_INTERVAL = "idleClientCleanInterval";
    static final String CONF_KEY_IDLE_CLIENT_TIMEOUT = "idleClientTimeout";

    private String s3SignerOverride = DEFAULT_SIGNER_OVERRIDE;
    private int socketTimeout = ClientConfiguration.DEFAULT_SOCKET_TIMEOUT;
    private int maxConnection = ClientConfiguration.DEFAULT_MAX_CONNECTIONS;
    private long connectionTTL = ClientConfiguration.DEFAULT_CONNECTION_TTL;
    private int connectionTimeout = ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT;
    private int maxErrorRetry = PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY;
    private boolean useExpectContinue = false;

    private int idleClientCleanInterval = 10 * 60 * 1000;
    private int idleClientTimeout = 20 * 60 * 1000;

    public CephS3ConnectionConf(Map<String, String> confProp, String prefix) {
        if (confProp.containsKey(prefix + CONF_KEY_SIGNER_OVERRIDE)) {
            s3SignerOverride = confProp.get(prefix + CONF_KEY_SIGNER_OVERRIDE);
        }
        if (confProp.containsKey(prefix + CONF_KEY_SOCKET_TIMEOUT)) {
            socketTimeout = Integer.parseInt(confProp.get(prefix + CONF_KEY_SOCKET_TIMEOUT));
            if (socketTimeout < 0) {
                throw new IllegalArgumentException("ceph s3 " + prefix + CONF_KEY_SOCKET_TIMEOUT
                        + " must be greater than or equals 0: " + socketTimeout);
            }
        }
        if (confProp.containsKey(prefix + CONF_KEY_CONN_TIMEOUT)) {
            connectionTimeout = Integer.parseInt(confProp.get(prefix + CONF_KEY_CONN_TIMEOUT));
            if (connectionTimeout < 0) {
                throw new IllegalArgumentException("ceph s3 " + prefix + CONF_KEY_CONN_TIMEOUT
                        + " must be greater than or equals 0: " + connectionTimeout);
            }
        }
        if (confProp.containsKey(prefix + CONF_KEY_CONN_TTL)) {
            connectionTTL = Integer.parseInt(confProp.get(prefix + CONF_KEY_CONN_TTL));
            if (connectionTTL < 0 && connectionTTL != -1) {
                throw new IllegalArgumentException("ceph s3 " + prefix + CONF_KEY_CONN_TTL
                        + " must be greater than or equals 0 , or equals -1: " + connectionTTL);
            }
        }
        if (confProp.containsKey(prefix + CONF_KEY_MAX_CONNS)) {
            maxConnection = Integer.parseInt(confProp.get(prefix + CONF_KEY_MAX_CONNS));
            if (maxConnection <= 0) {
                throw new IllegalArgumentException("ceph s3 " + prefix + CONF_KEY_MAX_CONNS
                        + " must be greater than 0: " + maxConnection);
            }
        }
        if (confProp.containsKey(prefix + CONF_KEY_MAX_ERROR_RETRY)) {
            maxErrorRetry = Integer.parseInt(confProp.get(prefix + CONF_KEY_MAX_ERROR_RETRY));
            if (maxErrorRetry < 0) {
                throw new IllegalArgumentException("ceph s3 " + prefix + CONF_KEY_MAX_ERROR_RETRY
                        + " must be greater than 0: " + maxErrorRetry);
            }
        }
        if (confProp.containsKey(prefix + CONF_KEY_USE_EXPECT_CONTINUE)) {
            useExpectContinue = Boolean
                    .parseBoolean(confProp.get(prefix + CONF_KEY_USE_EXPECT_CONTINUE));
        }

        if (confProp.containsKey(prefix + CONF_KEY_IDLE_CLIENT_CLEAN_INTERVAL)) {
            idleClientCleanInterval = Integer
                    .parseInt(confProp.get(prefix + CONF_KEY_IDLE_CLIENT_CLEAN_INTERVAL));
            if (idleClientCleanInterval < 0) {
                throw new IllegalArgumentException(
                        "ceph s3 " + prefix + CONF_KEY_IDLE_CLIENT_CLEAN_INTERVAL
                                + " must be greater than 0: " + idleClientCleanInterval);
            }
        }

        if (confProp.containsKey(prefix + CONF_KEY_IDLE_CLIENT_TIMEOUT)) {
            idleClientTimeout = Integer
                    .parseInt(confProp.get(prefix + CONF_KEY_IDLE_CLIENT_TIMEOUT));
            if (idleClientTimeout < 0) {
                throw new IllegalArgumentException(
                        "ceph s3 " + prefix + CONF_KEY_IDLE_CLIENT_TIMEOUT
                                + " must be greater than 0: " + idleClientTimeout);
            }
        }
    }

    public int getMaxErrorRetry() {
        return maxErrorRetry;
    }

    public CephS3ConnectionConf(Map<String, String> cephS3Conf) {
        this(cephS3Conf, "client.");
    }

    public String getS3SignerOverride() {
        return s3SignerOverride;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public int getMaxConnection() {
        return maxConnection;
    }

    public long getConnectionTTL() {
        return connectionTTL;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public boolean isUseExpectContinue() {
        return useExpectContinue;
    }

    public int getIdleClientCleanInterval() {
        return idleClientCleanInterval;
    }

    public int getIdleClientTimeout() {
        return idleClientTimeout;
    }

    @Override
    public String toString() {
        return "CephS3ConnectionConf{" + "s3SignerOverride='" + s3SignerOverride + '\''
                + ", socketTimeout=" + socketTimeout + ", maxConnection=" + maxConnection
                + ", connectionTTL=" + connectionTTL + ", connectionTimeout=" + connectionTimeout
                + ", maxErrorRetry=" + maxErrorRetry + ", useExpectContinue=" + useExpectContinue
                + ", idleClientCleanInterval=" + idleClientCleanInterval + ", idleClientTimeout="
                + idleClientTimeout + '}';
    }
}
