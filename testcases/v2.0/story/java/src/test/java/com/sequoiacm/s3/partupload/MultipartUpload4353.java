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
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-4353:上传分段，其中指定key和uploadId不一致
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4353 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4353";
    private String keyNameA = "key4353a";
    private String keyNameB = "key4353b";
    private AmazonS3 s3Client = null;
    private long fileSize = 10 * 1024 * 1024;
    private File localPath = null;
    private File file = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        file = new File( filePath );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
        s3Client.shutdown();
    }

    // SEQUOIACM-852问题单未修改，暂时屏蔽用例
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        testExpectContinue( true );
        testExpectContinue( false );
        runSuccess = true;
    }

    private void testExpectContinue( boolean useExpectContinue )
            throws Exception {
        AmazonS3 s3Client = null;
        try {
            if ( useExpectContinue ) {
                s3Client = buildS3ClientUseExpectContinue();
            } else {
                s3Client = S3Utils.buildS3Client();
            }
            testUpload( s3Client );
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void testUpload( AmazonS3 s3 ) {
        // 对象A正在上传
        List< PartETag > partEtags = new ArrayList<>();
        String uploadIdA = uploadObjectA( s3, partEtags );

        // 初始化对象B
        PartUploadUtils.initPartUpload( s3, bucketName, keyNameB );
        String wrongUploadId = "18714";
        // 上传分段指定uploadId不存在
        try {
            PartUploadUtils.partUpload( s3, bucketName, keyNameB, wrongUploadId,
                    file );
            Assert.fail(
                    "upload part with non-existent uploadId should fail." );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchUpload" );
        }

        // 上传分段指定uploadId为对象A的uploadId
        try {
            PartUploadUtils.partUpload( s3, bucketName, keyNameB, uploadIdA,
                    file );
            Assert.fail(
                    "upload part with uploadId of other keys should fail." );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchUpload" );
        }

        // 对象A完成分段上传
        PartUploadUtils.completeMultipartUpload( s3, bucketName, keyNameA,
                uploadIdA, partEtags );
        Assert.assertFalse( s3.doesObjectExist( bucketName, keyNameB ) );
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
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

    private String uploadObjectA( AmazonS3 s3, List< PartETag > partEtags ) {
        String uploadId = PartUploadUtils.initPartUpload( s3, bucketName,
                keyNameA );
        long partSize = 5 * 1024;
        UploadPartRequest partRequest = new UploadPartRequest().withFile( file )
                .withFileOffset( 0 ).withPartNumber( 1 )
                .withPartSize( partSize ).withBucketName( bucketName )
                .withKey( keyNameA ).withUploadId( uploadId );
        UploadPartResult uploadPartResult = s3.uploadPart( partRequest );
        partEtags.add( uploadPartResult.getPartETag() );
        return uploadId;
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
