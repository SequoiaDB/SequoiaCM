package com.sequoiacm.s3import.config;

public class S3ServerInfo {

    private String url;
    private String accessKey;
    private String secretKey;
    private String keyFilePath;
    private S3ConnectionConf s3ConnectionConf = new S3ConnectionConf();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getKeyFilePath() {
        return keyFilePath;
    }

    public void setKeyFilePath(String keyFilePath) {
        this.keyFilePath = keyFilePath;
    }

    public S3ConnectionConf getS3ConnectConf() {
        return s3ConnectionConf;
    }
}
