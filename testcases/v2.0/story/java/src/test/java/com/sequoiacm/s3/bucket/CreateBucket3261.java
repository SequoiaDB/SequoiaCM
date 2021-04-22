package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @Descreption SCM-3261:相同用户创建桶，部分桶名相同（标准模式）
 * @Author YiPan
 * @Date 2020/3/4
 */
public class CreateBucket3261 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;
    private String bucketNameA = "bucket3261";
    private String bucketNameB = "bucket3261";
    private String bucketNameC = "bucket3261c";

    @BeforeClass
    private void setUp() {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketNameA );
        S3Utils.clearBucket( s3Client, bucketNameC );
    }

    @Test
    public void test() {
        // 创建桶A
        s3Client.createBucket( bucketNameA );
        // 创建桶B
        try {
            s3Client.createBucket( bucketNameB );
            Assert.fail( "expect fail but success" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "BucketAlreadyOwnedByYou" );
        }
        // 创建桶C
        s3Client.createBucket( bucketNameC );
        // 查询结果校验
        int num = 0;
        List< Bucket > buckets = s3Client.listBuckets();
        for ( Bucket s : buckets ) {
            if ( s.getName().equals( bucketNameA )
                    || s.getName().equals( bucketNameC ) ) {
                num++;
            }
        }
        Assert.assertEquals( num, 2 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            S3Utils.clearBucket( s3Client, bucketNameA );
            S3Utils.clearBucket( s3Client, bucketNameC );
            s3Client.shutdown();
        }
    }
}
