package com.sequoiacm.s3import.client;

public class ScmS3ClientBuilder {
    private String accessKeyId;
    private String secretKeyId;
    private String endpoint;
    private int connectionRequestTimeout;
    private int connectTimeout;
    private int readTimeout;
    private boolean requestBody;

    private ScmS3ClientBuilder() {
        connectionRequestTimeout = 10000;
        connectTimeout = 10000;
        readTimeout = 10000;
        requestBody = false;
    }

    public static ScmS3ClientBuilder standard() {
        return new ScmS3ClientBuilder();
    }

    public ScmS3ClientBuilder withEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public ScmS3ClientBuilder withAccessKeys(String accessKey, String secretKey) {
        this.accessKeyId = accessKey;
        this.secretKeyId = secretKey;
        return this;
    }

    public ScmS3ClientBuilder withConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
        return this;
    }

    public ScmS3ClientBuilder withConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public ScmS3ClientBuilder withSocketTimeout(int socketTimeout) {
        this.readTimeout = socketTimeout;
        return this;
    }

    String getAccessKeyId() {
        return accessKeyId;
    }

    String getEndpoint() {
        return endpoint;
    }

    String getSecretKeyId() {
        return secretKeyId;
    }

    int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    int getConnectTimeout() {
        return connectTimeout;
    }

    int getReadTimeout() {
        return readTimeout;
    }

    boolean isRequestBody() {
        return requestBody;
    }

    public ScmS3Client build() {
        if (this.endpoint == null) {
            throw new IllegalArgumentException("Need endpoint.");
        }
        if (this.accessKeyId == null || this.secretKeyId == null) {
            throw new IllegalArgumentException("Need keys.");
        }
        return new ScmS3Client(this);
    }
}
