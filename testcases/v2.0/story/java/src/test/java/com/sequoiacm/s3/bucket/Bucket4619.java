package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @descreption SCM-4619 :: 桶禁用版本控制
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket4619 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;
    private final String bucketNameA = "bucket4619a";
    private final String bucketNameB = "bucket4619b";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketNameA );
        S3Utils.clearBucket( s3Client, bucketNameB );
    }

    @Test
    public void test() throws Exception {
        s3Client.createBucket( bucketNameA );
        s3Client.createBucket( bucketNameB );
        Assert.assertTrue( s3Client.doesBucketExistV2( bucketNameA ) );
        Assert.assertTrue( s3Client.doesBucketExistV2( bucketNameB ) );

        S3Utils.setBucketVersioning( s3Client, bucketNameA, "Suspended" );
        Assert.assertEquals( s3Client
                .getBucketVersioningConfiguration( bucketNameA ).getStatus(),
                "Suspended" );

        S3Utils.setBucketVersioning( s3Client, bucketNameB, "Enabled" );
        S3Utils.setBucketVersioning( s3Client, bucketNameB, "Suspended" );
        Assert.assertEquals( s3Client
                .getBucketVersioningConfiguration( bucketNameB ).getStatus(),
                "Suspended" );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketNameA );
                S3Utils.clearBucket( s3Client, bucketNameB );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }
}
