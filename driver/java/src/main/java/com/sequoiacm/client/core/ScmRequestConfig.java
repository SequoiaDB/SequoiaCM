package com.sequoiacm.client.core;

/**
 * Scm request config.
 */
public class ScmRequestConfig {
    // ms
    private int socketTimeout;

    private ScmRequestConfig(int socketTimeout) {
        this.socketTimeout = socketTimeout;
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
     * For build ScmRequestConfig.
     */
    public static class Builder {
        private int socketTimeout = 1800000;

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
         * Builds a instance of ScmRequestConfig.
         *
         * @return ScmRequestConfig.
         */
        public ScmRequestConfig build() {
            return new ScmRequestConfig(socketTimeout);
        }
    }
}
