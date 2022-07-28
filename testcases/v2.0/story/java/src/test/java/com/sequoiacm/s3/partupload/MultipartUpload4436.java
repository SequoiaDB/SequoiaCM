package com.sequoiacm.s3.partupload;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.util.Base64;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description SCM-4436:UploadPartRequest接口参数校验
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4436 extends TestScmBase {
    private final String bucketName = "bucket4436";
    private AmazonS3 s3Client = null;
    private File localPath = null;
    private final File[] file = new File[ 2 ];
    private final String[] filePath = new String[ 2 ];
    private final AtomicInteger actSuccessTests = new AtomicInteger( 0 );

    @DataProvider(name = "legalKeyNameProvider")
    public Object[][] generateKeyName() {
        return new Object[][] {
                // test a : 范围内取值， partNumber:10，桶已存在,file为已存在的文件，文件长度大于0
                new Object[] { "/dir1/test4436.txt", 10, 0 },
                // test b : 长度边界值:1字节 ，partNumber:1,file为已存在的文件，文件长度等于0
                new Object[] { TestTools.getRandomString( 1 ), 1, 1 },
                // test c : 长度边界值:900字节 ，partNumber:10000,file为已存在的文件，文件长度大于0
                new Object[] { TestTools.getRandomString( 900 ), 10000, 0 } };
    }

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        long fileSize = 10 * 1024 * 1024;
        filePath[ 0 ] = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        filePath[ 1 ] = localPath + File.separator + "localFile_0" + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath[ 0 ], fileSize );
        TestTools.LocalFile.createFile( filePath[ 1 ], 0 );
        file[ 0 ] = new File( filePath[ 0 ] );
        file[ 1 ] = new File( filePath[ 1 ] );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        s3Client.shutdown();
    }

    @Test(groups = { "oneSite", "twoSite",
            "fourSite" }, dataProvider = "legalKeyNameProvider")
    public void testUseExpectContinue1( String keyName, int partNumber,
            int fileIndex ) throws Exception {
        s3Client = buildS3ClientUseExpectContinue();
        testLegalKeyName( keyName, partNumber, fileIndex );
        s3Client.shutdown();
        actSuccessTests.getAndIncrement();
    }

    // SEQUOIACM-852问题单未修改，暂时屏蔽用例
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void testUseExpectContinue2() throws Exception {
        s3Client = buildS3ClientUseExpectContinue();
        testIllLegalParameter();
        s3Client.shutdown();
        actSuccessTests.getAndIncrement();
    }

    @Test(groups = { "oneSite", "twoSite",
            "fourSite" }, dataProvider = "legalKeyNameProvider")
    public void testNotUseExpectContinue1( String keyName, int partNumber,
            int fileIndex ) throws Exception {
        s3Client = S3Utils.buildS3Client();
        testLegalKeyName( keyName, partNumber, fileIndex );
        actSuccessTests.getAndIncrement();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void testNotUseExpectContinue2() throws Exception {
        s3Client = S3Utils.buildS3Client();
        testIllLegalParameter();
        s3Client.shutdown();
        actSuccessTests.getAndIncrement();
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( actSuccessTests
                    .get() == ( generateKeyName().length * 2 + 2 ) ) {
                s3Client = S3Utils.buildS3Client();
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    public void testIllLegalParameter() {
        testIllegalKeyName();
        testIllegalPartNumber();
        testIllegalBucketName();
        testIllegalFile();
        testIllegalMd5();
    }

    public void testLegalKeyName( String keyName, int partNumber,
            int fileIndex ) throws Exception {
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        List< PartETag > partEtags = new ArrayList<>();
        int filePosition = 0;
        long fileSize = file[ fileIndex ].length();
        String md5 = getMD5Digest( file[ fileIndex ] );
        UploadPartRequest partRequest = new UploadPartRequest()
                .withFile( file[ fileIndex ] ).withFileOffset( filePosition )
                .withPartNumber( partNumber ).withPartSize( fileSize )
                .withBucketName( bucketName ).withKey( keyName )
                .withUploadId( uploadId ).withMD5Digest( md5 );
        UploadPartResult uploadPartResult = s3Client.uploadPart( partRequest );
        partEtags.add( uploadPartResult.getPartETag() );

        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId, partEtags );

        String expMd5 = TestTools.getMD5( filePath[ fileIndex ] );
        String downloadMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downloadMd5, expMd5 );
    }

    private void testIllegalKeyName() {
        // test a : 对象名为空串，null，901个字节
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                "key4436" );
        int filePosition = 0;
        long fileSize = file[ 0 ].length();
        UploadPartRequest partRequest = new UploadPartRequest()
                .withFile( file[ 0 ] ).withFileOffset( filePosition )
                .withPartNumber( 1 ).withPartSize( fileSize )
                .withBucketName( bucketName ).withKey( "" )
                .withUploadId( uploadId );
        try {
            s3Client.uploadPart( partRequest );
            Assert.fail( "when key name is '',it should fail" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "InvalidRequest" );
        }

        partRequest = new UploadPartRequest().withFile( file[ 0 ] )
                .withFileOffset( filePosition ).withPartNumber( 1 )
                .withPartSize( fileSize ).withBucketName( bucketName )
                .withKey( null ).withUploadId( uploadId );
        try {
            s3Client.uploadPart( partRequest );
            Assert.fail( "when key name is null,it should fail" );
        } catch ( IllegalArgumentException e ) {
            Assert.assertEquals( e.getMessage(),
                    "The key parameter must be specified when uploading a part" );
        }

        partRequest = new UploadPartRequest().withFile( file[ 0 ] )
                .withFileOffset( filePosition ).withPartNumber( 1 )
                .withPartSize( fileSize ).withBucketName( bucketName )
                .withKey( TestTools.getRandomString( 901 ) )
                .withUploadId( uploadId );
        try {
            s3Client.uploadPart( partRequest );
            Assert.fail( "when key name is 901 characters,it should fail" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorMessage(),
                    "The specified multipart upload does not exist. The upload ID might be invalid, or the multipart upload might have been aborted or completed." );
        }
    }

    private void testIllegalPartNumber() {
        String keyName = "testkey4436";
        // test a : partNumber超过边界值
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        int filePosition = 0;
        long fileSize = file[ 0 ].length();
        UploadPartRequest partRequest = new UploadPartRequest()
                .withFile( file[ 0 ] ).withFileOffset( filePosition )
                .withPartNumber( 0 ).withPartSize( fileSize )
                .withBucketName( bucketName ).withKey( keyName )
                .withUploadId( uploadId );
        try {
            s3Client.uploadPart( partRequest );
            Assert.fail( "when partNumber is 0,it should fail" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "InvalidPartNumber" );
        }

        partRequest = new UploadPartRequest().withFile( file[ 0 ] )
                .withFileOffset( filePosition ).withPartNumber( 10001 )
                .withPartSize( fileSize ).withBucketName( bucketName )
                .withKey( keyName ).withUploadId( uploadId );
        try {
            s3Client.uploadPart( partRequest );
            Assert.fail( "when partNumber is 10001,it should fail" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "InvalidPartNumber" );
        }
    }

    private void testIllegalBucketName() {
        // test a : 桶不存在，为null
        String nonexistBucket = "nonexistentbucket18808";
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                "key4436" );
        int filePosition = 0;
        long fileSize = file[ 0 ].length();
        UploadPartRequest partRequest = new UploadPartRequest()
                .withFile( file[ 0 ] ).withFileOffset( filePosition )
                .withPartNumber( 1 ).withPartSize( fileSize )
                .withBucketName( nonexistBucket ).withKey( "key4436" )
                .withUploadId( uploadId );
        try {
            s3Client.uploadPart( partRequest );
            Assert.fail( "when bucket does not exist,it should fail" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchBucket" );
        }

        partRequest = new UploadPartRequest().withFile( file[ 0 ] )
                .withFileOffset( filePosition ).withPartNumber( 1 )
                .withPartSize( fileSize ).withBucketName( "" )
                .withKey( "key4436" ).withUploadId( uploadId );
        try {
            s3Client.uploadPart( partRequest );
            Assert.fail( "when bucketName is '',it should fail" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "InvalidRequest" );
        }
    }

    private void testIllegalFile() {
        // test a : file文件不存在
        String filePath = localPath + File.separator + "notexistfile4436.txt";
        File file = new File( filePath );
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                "key4436" );
        int filePosition = 0;
        long fileSize = file.length();
        UploadPartRequest partRequest = new UploadPartRequest().withFile( file )
                .withFileOffset( filePosition ).withPartNumber( 1 )
                .withPartSize( fileSize ).withBucketName( bucketName )
                .withKey( "key4436" ).withUploadId( uploadId );
        try {
            s3Client.uploadPart( partRequest );
            Assert.fail( "when file does not exist,it should fail" );
        } catch ( IllegalArgumentException e ) {
            Assert.assertTrue( e.getMessage().contains( "Failed to open file" ),
                    e.getMessage() );
        }

        // test b : file为null
        partRequest = new UploadPartRequest().withFile( null )
                .withFileOffset( filePosition ).withPartNumber( 1 )
                .withPartSize( fileSize ).withBucketName( bucketName )
                .withKey( "key4436" ).withUploadId( uploadId );
        try {
            s3Client.uploadPart( partRequest );
            Assert.fail( "when file is null,it should fail" );
        } catch ( IllegalArgumentException e ) {
            Assert.assertEquals( e.getMessage(),
                    "A File or InputStream must be specified when uploading part" );
        }
    }

    private void testIllegalMd5() {
        // test a : withMD5Digest取值和上传分段不一致
        String md5Digest = "123456789";
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                "key4436" );
        long fileSize = file[ 0 ].length();
        UploadPartRequest partRequest = new UploadPartRequest()
                .withFile( file[ 0 ] ).withFileOffset( 0 ).withPartNumber( 1 )
                .withPartSize( fileSize ).withBucketName( bucketName )
                .withKey( "key4436" ).withUploadId( uploadId )
                .withMD5Digest( md5Digest );
        try {
            s3Client.uploadPart( partRequest );
            Assert.fail( "when md5 digest is wrong,it should fail" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "InvalidDigest" );
        }
    }

    private String getMD5Digest( File file ) throws IOException {
        FileInputStream fileInputStream = null;
        int length = ( int ) file.length();
        try {
            MessageDigest md5 = MessageDigest.getInstance( "MD5" );
            fileInputStream = new FileInputStream( file );
            byte[] buffer = new byte[ length ];
            if ( fileInputStream.read( buffer ) != -1 ) {
                md5.update( buffer, 0, length );
            }

            byte[] digest = md5.digest();
            // 请求中携带md5需经过base64加密
            return Base64.encodeAsString( digest );
        } catch ( Exception e ) {
            e.printStackTrace();
            return null;
        } finally {
            if ( fileInputStream != null ) {
                fileInputStream.close();
            }
        }
    }

    private AmazonS3 buildS3ClientUseExpectContinue() throws Exception {
        AmazonS3 s3Client;
        AWSCredentials credentials = new BasicAWSCredentials(
                TestScmBase.s3AccessKeyID, TestScmBase.s3SecretKey );
        String clientRegion = "us-east-1";
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                S3Utils.getS3Url(), clientRegion );
        ClientConfiguration config = new ClientConfiguration();
        config.setUseExpectContinue( true );
        config.setSocketTimeout( 300000 );
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
}