package com.sequoiacm.common;

import java.util.Objects;

import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;

/**
 * ceph S3 user info
 */
public class CephS3UserInfo {
    private String accessKey;
    private String secretKeyFilePath;
    private String encryptedSecretKey;

    public CephS3UserInfo(String accessKey, String secretKeyFilePath) {
        this.accessKey = accessKey;
        this.secretKeyFilePath = secretKeyFilePath;
        this.encryptedSecretKey = ScmFilePasswordParser.parserFile(secretKeyFilePath)
                .getEncryptedPassword();
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKeyFilePath() {
        return secretKeyFilePath;
    }

    public void setSecretKeyFilePath(String secretKeyFilePath) {
        this.secretKeyFilePath = secretKeyFilePath;
    }

    public String getEncryptedSecretKey() {
        return encryptedSecretKey;
    }

    public void setEncryptedSecretKey(String encryptedSecretKey) {
        this.encryptedSecretKey = encryptedSecretKey;
    }

    public String getSecretKey() {
        try {
            return ScmPasswordMgr.getInstance().decrypt(ScmPasswordMgr.SCM_CRYPT_TYPE_DES,
                    encryptedSecretKey);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(
                    "failed to decrypt password file: encryptedSecretKey=" + encryptedSecretKey, e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CephS3UserInfo that = (CephS3UserInfo) o;
        return Objects.equals(accessKey, that.accessKey)
                && Objects.equals(secretKeyFilePath, that.secretKeyFilePath)
                && Objects.equals(encryptedSecretKey, that.encryptedSecretKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessKey, secretKeyFilePath, encryptedSecretKey);
    }

    @Override
    public String toString() {
        return "CephS3UserInfo{" + "accessKey='" + accessKey + '\'' + ", secretKeyFilePath='"
                + secretKeyFilePath + '\'' + '}';
    }
}
