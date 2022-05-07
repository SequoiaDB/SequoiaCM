package com.sequoiacm.s3import.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.client.ScmS3Client;
import com.sequoiacm.s3import.client.ScmS3ClientBuilder;
import com.sequoiacm.s3import.exception.S3ImportExitCode;

public class S3ClientManager {

    private static volatile S3ClientManager INSTANCE = null;
    private AmazonS3Client srcS3Client;
    private AmazonS3Client destS3Client;
    private ScmS3Client destScmS3Client;

    private S3ClientManager() throws ScmToolsException {
        ImportToolProps toolProps = ImportToolProps.getInstance();
        srcS3Client = initConnection(toolProps.getSrcS3());
        destS3Client = initConnection(toolProps.getDestS3());
        destScmS3Client = initScmS3Client(toolProps.getDestS3());
    }

    private AmazonS3Client initConnection(S3ServerInfo s3Info) throws ScmToolsException {
        AmazonS3Client conn = null;
        try {
            AWSCredentials credentials = new BasicAWSCredentials(s3Info.getAccessKey(),
                    s3Info.getSecretKey());
            ClientConfiguration conf = new ClientConfiguration();
            conf.setProtocol(Protocol.HTTP);

            S3ConnectionConf confProp = s3Info.getS3ConnectConf();

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

            conn = new AmazonS3Client(credentials, conf);
            S3ClientOptions op = new S3ClientOptions();
            op.setPathStyleAccess(true);
            conn.setS3ClientOptions(op);
            conn.setEndpoint(s3Info.getUrl());
            // try connect to s3.
            conn.getS3AccountOwner();
            return conn;
        }
        catch (Exception e) {
            if (conn != null) {
                conn.shutdown();
            }
            throw new ScmToolsException("Failed to init s3 connection: url=" + s3Info.getUrl()
                    + ", accessKey:" + s3Info.getAccessKey(), S3ImportExitCode.SYSTEM_ERROR, e);
        }
    }

    private ScmS3Client initScmS3Client(S3ServerInfo s3Info) {
        String endpoint = s3Info.getUrl();
        if (!endpoint.startsWith("http")) {
            endpoint = "http://" + endpoint;
        }
        S3ConnectionConf conf = s3Info.getS3ConnectConf();
        return ScmS3ClientBuilder.standard().withEndpoint(endpoint)
                .withAccessKeys(s3Info.getAccessKey(), s3Info.getSecretKey())
                .withConnectTimeout(conf.getConnectionTimeout())
                .withSocketTimeout(conf.getSocketTimeout()).build();
    }

    public static S3ClientManager getInstance() throws ScmToolsException {
        if (INSTANCE == null) {
            synchronized (S3ClientManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new S3ClientManager();
                }
            }
        }
        return INSTANCE;
    }

    public AmazonS3Client getSrcS3Client() {
        return srcS3Client;
    }

    public ScmS3Client getDestScmS3Client() {
        return destScmS3Client;
    }

    public AmazonS3Client getDestS3Client() {
        return destS3Client;
    }
}
