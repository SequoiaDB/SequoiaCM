package com.sequoiacm.cephs3.dataservice;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.sequoiacm.cephs3.CephS3Exception;

public class CephS3Conn {
    private static final Logger logger = LoggerFactory.getLogger(CephS3Conn.class);
    private final int siteId;
    private final String url;
    private AmazonS3Client conn = null;
    private AtomicInteger usingCount = new AtomicInteger();
    private long lastAccessTime = System.currentTimeMillis();
    private volatile boolean shouldBeDiscard = false;

    public CephS3Conn(int siteId, String accessKey, String secretKey, String url,
            CephS3ConnectionConf confProp) throws CephS3Exception {
        try {
            AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            ClientConfiguration conf = new ClientConfiguration();
            conf.setProtocol(Protocol.HTTP);

            if (confProp.getS3SignerOverride() != null
                    && !confProp.getS3SignerOverride().trim().equals("")) {
                conf.setSignerOverride(confProp.getS3SignerOverride());
            }
            conf.setSocketTimeout(confProp.getSocketTimeout());
            conf.setConnectionTimeout(confProp.getConnectionTimeout());
            conf.setConnectionTTL(confProp.getConnectionTTL());
            conf.setMaxConnections(confProp.getMaxConnection());
            conf.setRetryPolicy(new RetryPolicy(PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION,
                    PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY, confProp.getMaxErrorRetry(),
                    true));
            conf.setUseExpectContinue(confProp.isUseExpectContinue());
            conn = new AmazonS3Client(credentials, conf);
            S3ClientOptions op = new S3ClientOptions();
            op.setPathStyleAccess(true);
            conn.setS3ClientOptions(op);
            conn.setEndpoint(url);
            // try connect to s3.
            conn.getS3AccountOwner();
            this.siteId = siteId;
            this.url = url;
        }
        catch (Exception e) {
            if (conn != null) {
                conn.shutdown();
            }
            throw e;
        }
    }

    public String getUrl() {
        return url;
    }

    public int getSiteId() {
        return siteId;
    }

    public AmazonS3Client getAmzClient() {
        return conn;
    }

    public void shutdownSilence() {
        try {
            conn.shutdown();
        }
        catch (Exception e) {
            logger.warn("failed to shutdown connection:{}", url, e);
        }
    }

    public int getUsingCount() {
        return usingCount.get();
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void incUsingCount() {
        usingCount.incrementAndGet();
        lastAccessTime = System.currentTimeMillis();
    }

    public void decUsingCount() {
        usingCount.decrementAndGet();
    }

    boolean isShouldBeDiscard() {
        return this.shouldBeDiscard;
    }

    public void setShouldBeDiscard(boolean shouldBeDiscard) {
        this.shouldBeDiscard = shouldBeDiscard;
    }
}
