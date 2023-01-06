package com.sequoiacm.common;

import java.util.Objects;

public class CephS3UrlInfo {
    private final String url;
    private CephS3UserInfo userInfo;

    public CephS3UrlInfo(String url) {
        String[] elements = url.split("@");
        if (elements.length == 1) {
            this.url = url;
            return;
        }

        if (elements.length > 2) {
            throw new IllegalArgumentException("cephs3 data url syntax is invalid: " + url
                    + ", expected: accesskey:secretkeyFilePath@http://cephs3");
        }

        this.url = elements[1];

        String[] accesskeyAndSecretkeyFilePath = elements[0].split(":");
        if (accesskeyAndSecretkeyFilePath.length != 2) {
            throw new IllegalArgumentException("cephs3 data url syntax is invalid: " + url
                    + ", expected: accesskey:secretkeyFilePath@http://cephs3");
        }

        String accesskey = accesskeyAndSecretkeyFilePath[0];
        String secretkeyFilePath = accesskeyAndSecretkeyFilePath[1];
        userInfo = new CephS3UserInfo(accesskey, secretkeyFilePath);
    }

    public String getUrl() {
        return url;
    }

    public boolean hasAccesskeyAndSecretkey() {
        return userInfo != null;
    }

    public CephS3UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(CephS3UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CephS3UrlInfo that = (CephS3UrlInfo) o;
        return Objects.equals(url, that.url) && Objects.equals(userInfo, that.userInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, userInfo);
    }

    @Override
    public String toString() {
        return "CephS3UrlInfo{" + "url='" + url + '\'' + ", userInfo='" + userInfo + '\'' + '}';
    }
}
