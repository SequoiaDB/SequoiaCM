package com.sequoiacm.s3.object;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @descreption SCM-4965:通过预签名获取url，使用head object方式访问
 * @author ZhangYanan
 * @Date 2022/7/20
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4965 extends TestScmBase {
    private static String clientRegion = "us-east-1";
    private String bucketName = "bucket24256";
    private String keyName = "testkey_24256";
    private String file = "object24256";
    private AmazonS3 s3ClientV4 = null;
    private AmazonS3 s3ClientV2 = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3ClientV4 = S3Utils.buildS3Client();
        s3ClientV2 = buildS3ClientV2();
        S3Utils.clearBucket( s3ClientV4, bucketName );

        s3ClientV4.createBucket( bucketName );
        s3ClientV4.putObject( bucketName, keyName, file );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis( new Date().getTime() + 1000 * 10 );
        Date expireTime = calendar.getTime();

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                bucketName, keyName ).withExpiration( expireTime )
                        .withMethod( com.amazonaws.HttpMethod.HEAD );

        URL urlv4 = s3ClientV4.generatePresignedUrl( request );
        URL urlv2 = s3ClientV2.generatePresignedUrl( request );
        checkResponse( urlv4 );
        checkResponse( urlv2 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3ClientV4, bucketName );
            }
        } finally {
            if ( s3ClientV2 != null ) {
                s3ClientV2.shutdown();
            }
            if ( s3ClientV4 != null ) {
                s3ClientV4.shutdown();
            }
        }
    }

    private AmazonS3 buildS3ClientV2() {
        AmazonS3 s3Client;
        AWSCredentials credentials = new BasicAWSCredentials(
                TestScmBase.s3AccessKeyID, TestScmBase.s3SecretKey );
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                S3Utils.getS3Url(), clientRegion );
        ClientConfiguration config = new ClientConfiguration();
        config.setUseExpectContinue( false );
        config.setSocketTimeout( 300000 );
        // 以v2方式生成连接
        config.setSignerOverride( "S3SignerType" );
        s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration( endpointConfiguration )
                .withClientConfiguration( config )
                .withChunkedEncodingDisabled( true )
                .withPathStyleAccessEnabled( true )
                .withCredentials(
                        new AWSStaticCredentialsProvider( credentials ) )
                .build();
        return s3Client;
    }

    private void checkResponse( URL url ) throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        RequestEntity< ? > requestEntity = new RequestEntity<>( HttpMethod.HEAD,
                new URI( url.toString() ) );
        ResponseEntity< Resource > resp = restTemplate.exchange( requestEntity,
                Resource.class );
        Assert.assertEquals( resp.getStatusCode(), HttpStatus.OK );
    }
}
