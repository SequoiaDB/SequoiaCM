package com.sequoiacm.auth;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.util.Base64;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-3619:sign_info所有参数正确，进行登录
 * @Author YiPan
 * @Date 2021/4/21
 */
public class S3AuthServer3619 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String username = "user3619";
    private String password = "user3619123456";
    private String bucketName = "bucket3619";
    private AmazonS3 amazonS3 = null;
    private String[] accessKeys = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ScmAuthUtils.createAdminUserGrant( session, s3WorkSpaces, username,
                password );
        accessKeys = ScmAuthUtils.refreshAccessKey( session, username, password,
                null );
        ScmAuthUtils.checkPriorityByS3( accessKeys, s3WorkSpaces );
    }

    @Test(enabled = false)
    private void test() throws Exception {
        // 计算签名
        String algorithm = "HmacSHA256";
        String[] data = { "1", "2", "3", "4", "5" };
        byte[] kSecret = accessKeys[ 1 ].getBytes();
        byte[] kDate = ScmAuthUtils.sign( data[ 0 ], kSecret, algorithm );
        byte[] kRegion = ScmAuthUtils.sign( data[ 1 ], kDate, algorithm );
        byte[] kService = ScmAuthUtils.sign( data[ 2 ], kRegion, algorithm );
        byte[] kSigning = ScmAuthUtils.sign( data[ 3 ], kService, algorithm );
        byte[] ksignature = ScmAuthUtils.sign( data[ 4 ], kSigning, algorithm );
        String signatureClient = Base64.encodeAsString( ksignature );
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "signature", signatureClient );
        signInfo.put( "string_to_sign", data );

        // v4登录做业务操作
        amazonS3 = buildS3Client( accessKeys[ 0 ], accessKeys[ 1 ], "v4" );
        amazonS3.createBucket( bucketName );
        amazonS3.deleteBucket( bucketName );
        amazonS3.shutdown();

        // v2登录做业务操作
        amazonS3 = buildS3Client( accessKeys[ 0 ], accessKeys[ 1 ], "v2" );
        amazonS3.createBucket( bucketName );
        amazonS3.deleteBucket( bucketName );
        amazonS3.shutdown();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ScmFactory.User.deleteUser( session, username );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    public static AmazonS3 buildS3Client( String ACCESS_KEY, String SECRET_KEY,
            String version ) throws Exception {
        String clientRegion = "us-east-1";
        AWSCredentials credentials = new BasicAWSCredentials( ACCESS_KEY,
                SECRET_KEY );
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                S3Utils.getS3Url(), clientRegion );
        ClientConfiguration config = new ClientConfiguration();
        config.setUseExpectContinue( false );
        config.setSocketTimeout( 300000 );
        if ( version.equals( "v2" ) ) {
            config.setProtocol( Protocol.HTTP );
            config.setSignerOverride( "S3SignerType" );
        }
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration( endpointConfiguration )
                .withClientConfiguration( config )
                .withChunkedEncodingDisabled( true )
                .withPathStyleAccessEnabled( true )
                .withCredentials(
                        new AWSStaticCredentialsProvider( credentials ) )
                .build();
        return s3Client;
    }
}
