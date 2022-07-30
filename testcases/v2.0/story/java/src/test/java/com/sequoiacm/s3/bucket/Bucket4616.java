package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @descreption SCM-4616 :: 桶版本控制状态参数校验
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket4616 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;
    private final String bucketName = "bucket4616";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws Exception {
        s3Client.createBucket( bucketName );
        Assert.assertTrue( s3Client.doesBucketExistV2( bucketName ) );

        Assert.assertEquals( s3Client
                .getBucketVersioningConfiguration( bucketName ).getStatus(),
                "Off" );

        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
        Assert.assertEquals( s3Client
                .getBucketVersioningConfiguration( bucketName ).getStatus(),
                "Enabled" );

        S3Utils.setBucketVersioning( s3Client, bucketName, "Suspended" );
        Assert.assertEquals( s3Client
                .getBucketVersioningConfiguration( bucketName ).getStatus(),
                "Suspended" );

        try {
            S3Utils.setBucketVersioning( s3Client, bucketName, "null" );
            Assert.fail( "exp fail but found success" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "InvalidVersioningStatus" );
        }

        try {
            S3Utils.setBucketVersioning( s3Client, bucketName, "abc" );
            Assert.fail( "exp fail but found success" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "InvalidVersioningStatus" );
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
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
