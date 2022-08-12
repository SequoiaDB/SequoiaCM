package com.sequoiacm.s3import.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.retry.PredefinedRetryPolicies;

public class S3ConnectionConf {

    static final String V2_SIGNER_OVERRIDE = "S3SignerType";
    static final String CONF_KEY_SIGNER_OVERRIDE = "signerOverride";
    static final String CONF_KEY_SOCKET_TIMEOUT = "socketTimeout";
    static final String CONF_KEY_CONN_TTL = "connTTL";
    static final String CONF_KEY_CONN_TIMEOUT = "connTimeout";
    static final String CONF_KEY_MAX_ERROR_RETRY = "maxErrorRetry";

    private String s3SignerOverride;
    private int socketTimeout = ClientConfiguration.DEFAULT_SOCKET_TIMEOUT;
    private int maxConnection = ClientConfiguration.DEFAULT_MAX_CONNECTIONS;
    private long connectionTTL = ClientConfiguration.DEFAULT_CONNECTION_TTL;
    private int connectionTimeout = ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT;
    private int maxErrorRetry = PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY;

    public boolean addConnectConf(String key, String value) {
        if (key.equals(CONF_KEY_SIGNER_OVERRIDE)) {
            if (value != null && !value.trim().equals("")){
                if (!value.equals("V2") && !value.equals("V4")){
                    throw new IllegalArgumentException("s3 " + CONF_KEY_SIGNER_OVERRIDE
                        +" out of range. valid range: V2/V4: " + value);
                }
                else if (value.equals("V2")){
                    this.s3SignerOverride = V2_SIGNER_OVERRIDE;
                }
            }
        }
        else if (key.equals(CONF_KEY_SOCKET_TIMEOUT)) {
            this.socketTimeout = Integer.parseInt(value);
            if (socketTimeout < 0) {
                throw new IllegalArgumentException("s3 " + CONF_KEY_SOCKET_TIMEOUT
                        + " must be greater than or equals 0: " + socketTimeout);
            }
        }
        else if (key.equals(CONF_KEY_CONN_TIMEOUT)) {
            this.connectionTimeout = Integer.parseInt(value);
            if (connectionTimeout < 0) {
                throw new IllegalArgumentException("s3 " + CONF_KEY_CONN_TIMEOUT
                        + " must be greater than or equals 0: " + connectionTimeout);
            }
        }
        else if (key.equals(CONF_KEY_CONN_TTL)) {
            this.connectionTTL = Integer.parseInt(value);
            if (connectionTTL < 0 && connectionTTL != -1) {
                throw new IllegalArgumentException("s3 " + CONF_KEY_CONN_TTL
                        + " must be greater than or equals 0 , or equals -1: " + connectionTTL);
            }
        }
        else if (key.equals(CONF_KEY_MAX_ERROR_RETRY)) {
            this.maxErrorRetry = Integer.parseInt(value);
            if (maxErrorRetry < 0) {
                throw new IllegalArgumentException("s3 " + CONF_KEY_MAX_ERROR_RETRY
                        + " must be greater than 0: " + maxErrorRetry);
            }
        }
        else {
            return false;
        }
        return true;
    }

    public String getS3SignerOverride() {
        return s3SignerOverride;
    }

    public void setS3SignerOverride(String s3SignerOverride) {
        this.s3SignerOverride = s3SignerOverride;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getMaxConnection() {
        return maxConnection;
    }

    public void setMaxConnection(int maxConnection) {
        this.maxConnection = maxConnection;
    }

    public long getConnectionTTL() {
        return connectionTTL;
    }

    public void setConnectionTTL(long connectionTTL) {
        this.connectionTTL = connectionTTL;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getMaxErrorRetry() {
        return maxErrorRetry;
    }

    public void setMaxErrorRetry(int maxErrorRetry) {
        this.maxErrorRetry = maxErrorRetry;
    }
}
