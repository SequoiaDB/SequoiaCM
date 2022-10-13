package com.sequoiacm.s3.object;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @descreption SCM-5352:使用1.11.343版本的aws-sdk-s3驱动包通过预签名的url获取对象
 * @author ZhangYanan
 * @Date 2022/7/20
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object5352 extends TestScmBase {
    private static String clientRegion = "us-east-1";
    private String bucketName = "bucket5352";
    private String keyName = "testkey_5352";
    private String file = "object5352";
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

        URL urlV4 = s3ClientV4.generatePresignedUrl( request );
        URL urlV2 = s3ClientV2.generatePresignedUrl( request );

        String[] urlV2Str = urlV2.toString().split( "&" );
        String urlV2String = "";
        String urlV2Expires = null;
        for ( String str : urlV2Str ) {
            if ( !str.contains( "Expires" ) ) {
                urlV2String += str + "&";
            } else {
                urlV2Expires = str;
            }
        }
        urlV2String += urlV2Expires;

        String[] urlV4Str = urlV4.toString().split( "&" );
        String urlV4String = "";
        String urlV4Signature = null;
        for ( String str : urlV4Str ) {
            if ( !str.contains( "X-Amz-Signature" ) ) {
                urlV4String += str + "&";
            } else {
                urlV4Signature = str;
            }
        }
        urlV4String += urlV4Signature;

        checkResponse( urlV2String );
        checkResponse( urlV4String );

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

    private void checkResponse( String url ) throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        RequestEntity< ? > requestEntity = new RequestEntity<>( HttpMethod.HEAD,
                new URI( url ) );
        ResponseEntity< Resource > resp = restTemplate.exchange( requestEntity,
                Resource.class );
        Assert.assertEquals( resp.getStatusCode(), HttpStatus.OK );
    }
}
