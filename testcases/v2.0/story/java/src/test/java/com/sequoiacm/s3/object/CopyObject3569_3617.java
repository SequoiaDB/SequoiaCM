package com.sequoiacm.s3.object;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-3569:复制对象，源对象不存在 ; SCM-3617:指定桶不存在
 * @author fanyu
 * @Date 2019.09.18
 * @version 1.00
 */
public class CopyObject3569_3617 extends TestScmBase {
    private boolean runSuccess = false;
    private String srcKeyName = "src/object3569";
    private String destKeyName = "dest/object3569";
    private String bucketName = "bucket3569";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, srcKeyName, "test copy object." );
    }

    @Test
    public void testCopyObject() throws Exception {
        // test 3569 b: copy object with no versionId
        try {
            String keyName = "test3569";
            s3Client.copyObject( bucketName, keyName, bucketName, destKeyName );
            Assert.fail( "copyObject must be fail !" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchKey",
                    e.getStatusCode() + e.getErrorMessage() );
        }

        // testcase 3617: copy object with no srcBucket
        try {
            String srcBucketName = "test19326";
            s3Client.copyObject( srcBucketName, srcKeyName, bucketName,
                    destKeyName );
            Assert.fail( "copyObject must be fail !" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchBucket",
                    e.getStatusCode() + e.getErrorMessage() );
        }

        // testcase 3617: copy object with no destBucket
        try {
            String destBucketName = "test19326";
            s3Client.copyObject( bucketName, srcKeyName, destBucketName,
                    destKeyName );
            Assert.fail( "copyObject must be fail !" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchBucket",
                    e.getStatusCode() + e.getErrorMessage() );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }

}
