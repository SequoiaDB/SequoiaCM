package com.sequoiacm.testcommon;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;

public class S3Utils extends ScmTestMultiCenterBase {
    private static String clientRegion = "us-east-1";
    private static String gateway = getGatewayList().get(0);

    /**
     * @return
     * @throws Exception
     * @descreption 创建一个S3连接
     */
    public static AmazonS3 buildS3Client() throws Exception {
        return buildS3Client(ScmTestMultiCenterBase.getS3AccessKeyID(),
                ScmTestMultiCenterBase.getS3SecretKey(), getS3Url());
    }

    /**
     * @param ACCESS_KEY
     * @param SECRET_KEY
     * @return
     * @throws Exception
     * @descreption 使用指定ACCESS_KEY和SECRET_KEY连接
     */
    public static AmazonS3 buildS3Client(String ACCESS_KEY, String SECRET_KEY) throws Exception {
        return buildS3Client(ACCESS_KEY, SECRET_KEY, getS3Url());
    }

    /**
     * @param ACCESS_KEY
     * @param SECRET_KEY
     * @param S3URL
     * @return
     * @descreption 使用ACCESS_KEY和SECRET_KEY连接指定站点的S3节点
     */
    public static AmazonS3 buildS3Client(String ACCESS_KEY, String SECRET_KEY, String S3URL) {
        ClientConfiguration config = new ClientConfiguration();
        config.setUseExpectContinue(false);
        config.setSocketTimeout(300000);
        return buildS3Client(ACCESS_KEY, SECRET_KEY, S3URL, config);
    }

    /**
     * @param ACCESS_KEY
     * @param SECRET_KEY
     * @param S3URL
     * @return
     * @descreption 使用ACCESS_KEY和SECRET_KEY连接指定站点的S3节点
     */
    public static AmazonS3 buildS3Client(String ACCESS_KEY, String SECRET_KEY, String S3URL,
            ClientConfiguration config) {
        AWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                S3URL, clientRegion);
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration).withClientConfiguration(config)
                .withChunkedEncodingDisabled(true).withPathStyleAccessEnabled(true)
                .withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
        return s3Client;
    }

    /**
     * @return
     * @throws Exception
     * @descreption 根据网关生成s3 url
     */
    public static String getS3Url() {
        return "http://" + gateway;
    }
}
