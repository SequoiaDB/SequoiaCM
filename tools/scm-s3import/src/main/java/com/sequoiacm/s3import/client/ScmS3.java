package com.sequoiacm.s3import.client;

import com.sequoiacm.s3import.client.exception.ScmS3ClientException;

import java.util.Map;

public interface ScmS3 {

    void deleteObject(String bucketName, String key, Map<String, Object> deleteConf)
            throws ScmS3ClientException;

    void shutdown();
}
