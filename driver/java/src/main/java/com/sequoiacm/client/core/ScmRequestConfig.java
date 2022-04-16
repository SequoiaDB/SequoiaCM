package com.sequoiacm.client.core;

/**
 * Scm request config.
 */
public class ScmRequestConfig {
    // ms
    private int socketTimeout;

    // ms
    private int connectTimeout;

    private ScmRequestConfig(int socketTimeout, int connectTimeout) {
        this.socketTimeout = socketTimeout;
        this.connectTimeout = connectTimeout;
    }

    /**
     * Create a config builder.
     *
     * @return builder.
     */
    public static Builder custom() {
        return new Builder();
    }

    /**
     * Gets the socket timeout of the config.
     *
     * @return socket timeout.
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * Gets the connect timeout of the config.
     *
     * @return connect timeout.
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * For build ScmRequestConfig.
     */
    public static class Builder {
        private int socketTimeout = 1800000;
        private int connectTimeout = 10000;

        /**
         * Sets socket timeout.
         *
         * @param socketTimeout
         *            socket timeout.
         * @return current builder.
         */
        public Builder setSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        /**
         * Sets connect timeout.
         *
         * @param connectTimeout
         *            connect timeout.
         * @return current builder.
         */
        public Builder setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * Builds a instance of ScmRequestConfig.
         *
         * @return ScmRequestConfig.
         */
        public ScmRequestConfig build() {
            return new ScmRequestConfig(socketTimeout, connectTimeout);
        }
    }
}
