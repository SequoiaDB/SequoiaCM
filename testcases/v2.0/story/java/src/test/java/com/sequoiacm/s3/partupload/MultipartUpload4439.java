package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.PartETag;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description SCM-4439:CompleteMultipartUploadRequest接口参数校验
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4439 extends TestScmBase {
    private final String bucketName = "bucket4439";
    private AmazonS3 s3Client = null;
    private File localPath = null;
    private File file = null;
    private String filePath = null;
    private final AtomicInteger actSuccessTests = new AtomicInteger( 0 );

    @DataProvider(name = "legalKeyNameProvider")
    public Object[][] generateKeyName() {
        return new Object[][] { new Object[] { TestTools.getRandomString( 1 ) },
                new Object[] { TestTools.getRandomString( 900 ) } };
    }

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        long fileSize = 15 * 1024 * 1024;
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        file = new File( filePath );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(groups = { "oneSite", "twoSite",
            "fourSite" }, dataProvider = "legalKeyNameProvider")
    public void testLegalKeyName( String keyName ) throws Exception {
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        List< PartETag > partEtags = PartUploadUtils.partUpload( s3Client,
                bucketName, keyName, uploadId, file );

        // a.接口参数取值合法---已在功能测试中验证（此处覆盖对象名长度为1和900字节）
        PartUploadUtils.completeMultipartUpload( s3Client, bucketName, keyName,
                uploadId, partEtags );
        String expMd5 = TestTools.getMD5( filePath );
        String downloadMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downloadMd5, expMd5 );
        actSuccessTests.getAndIncrement();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void testIllegalParameter() {
        String keyName = "key4439";
        String uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName,
                keyName );
        List< PartETag > partEtags = PartUploadUtils.partUpload( s3Client,
                bucketName, keyName, uploadId, file );

        // b.接口参数取值非法---对象名为空串""，null，901个字节
        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(
                bucketName, "", uploadId, partEtags );
        try {
            s3Client.completeMultipartUpload( request );
            Assert.fail( "when keyName is '', it should fail." );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "InvalidRequest" );
        }

        request = new CompleteMultipartUploadRequest( bucketName, null,
                uploadId, partEtags );
        try {
            s3Client.completeMultipartUpload( request );
            Assert.fail( "when keyName is null, it should fail." );
        } catch ( IllegalArgumentException e ) {
            Assert.assertEquals( e.getMessage(),
                    "The key parameter must be specified when completing a multipart upload" );
        }

        request = new CompleteMultipartUploadRequest( bucketName,
                TestTools.getRandomString( 901 ), uploadId, partEtags );
        try {
            s3Client.completeMultipartUpload( request );
            Assert.fail( "when key name is 901 characters,it should fail" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchUpload" );
        }

        // 桶名为null
        request = new CompleteMultipartUploadRequest( null, keyName, uploadId,
                partEtags );
        try {
            s3Client.completeMultipartUpload( request );
            Assert.fail( "when bucketName is null, it should fail." );
        } catch ( IllegalArgumentException e ) {
            Assert.assertEquals( e.getMessage(),
                    "The bucket name parameter must be specified when completing a multipart upload" );
        }

        // uploadId为null
        request = new CompleteMultipartUploadRequest( bucketName, keyName, null,
                partEtags );
        try {
            s3Client.completeMultipartUpload( request );
            Assert.fail( "when uploadId is null, it should fail." );
        } catch ( IllegalArgumentException e ) {
            Assert.assertEquals( e.getMessage(),
                    "The upload ID parameter must be specified when completing a multipart upload" );
        }

        // partEtags为null
        request = new CompleteMultipartUploadRequest( bucketName, keyName,
                uploadId, null );
        try {
            s3Client.completeMultipartUpload( request );
            Assert.fail( "when partEtags is null, it should fail." );
        } catch ( IllegalArgumentException e ) {
            Assert.assertEquals( e.getMessage(),
                    "The part ETags parameter must be specified when completing a multipart upload" );
        }
        actSuccessTests.getAndIncrement();
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( actSuccessTests.get() == ( generateKeyName().length + 1 ) ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}