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
 * @descreption SCM-4617 :: 不存在桶，设置、查询桶版本控制策略
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket4617 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private final String bucketName = "bucket4617";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
    }

    @Test
    public void test() throws Exception {
        try {
            s3Client.getBucketVersioningConfiguration( bucketName ).getStatus();
            Assert.fail( "exp fail but act success" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchBucket" );
        }

        try {
            S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
            Assert.fail( "exp fail but act success" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchBucket" );
        }

        try {
            S3Utils.setBucketVersioning( s3Client, bucketName, "Suspended" );
            Assert.fail( "exp fail but act success" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchBucket" );
        }

        try {
            S3Utils.setBucketVersioning( s3Client, bucketName, "abc" );
            Assert.fail( "exp fail but act success" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "InvalidVersioningStatus" );
        }
    }

    @AfterClass
    private void tearDown() {
        if ( s3Client != null ) {
            s3Client.shutdown();
        }
    }
}
