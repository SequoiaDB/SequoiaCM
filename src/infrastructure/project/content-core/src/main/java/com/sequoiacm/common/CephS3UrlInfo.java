package com.sequoiacm.common;

import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;

import java.util.Objects;

public class CephS3UrlInfo {
    private final String url;
    private String accesskey;
    private String secretkey;

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
        accesskey = accesskeyAndSecretkeyFilePath[0];
        String secretkeyFilePath = accesskeyAndSecretkeyFilePath[1];
        AuthInfo auth = ScmFilePasswordParser.parserFile(secretkeyFilePath);
        secretkey = auth.getPassword();
    }

    public String getUrl() {
        return url;
    }

    public boolean hasAccesskeyAndSecretkey() {
        return accesskey != null && secretkey != null;
    }

    public String getAccesskey() {
        return accesskey;
    }

    public String getSecretkey() {
        return secretkey;
    }

    public void setAccesskey(String accesskey) {
        this.accesskey = accesskey;
    }

    public void setSecretkey(String secretkey) {
        this.secretkey = secretkey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CephS3UrlInfo that = (CephS3UrlInfo) o;
        return Objects.equals(url, that.url) && Objects.equals(accesskey, that.accesskey)
                && Objects.equals(secretkey, that.secretkey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, accesskey, secretkey);
    }

    @Override
    public String toString() {
        return "CephS3UrlInfo{" + "url='" + url + '\'' + ", accesskey='" + accesskey + '\'' + '}';
    }
}
