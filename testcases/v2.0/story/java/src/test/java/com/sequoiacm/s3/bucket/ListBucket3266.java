package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @Descreption SCM-3266:部分桶下有对象，列取桶列表信息（标准模式）
 * @Author YiPan
 * @Date 2021/3/5
 */
public class ListBucket3266 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;
    private String bucketNameA = "bucket3266a";
    private String bucketNameB = "bucket3266b";
    private String bucketNameC = "bucket3266c";
    private String bucketNameD = "bucket3266d";
    private String objectName = "object3260";
    private List< Bucket > existBuckets;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketNameA );
        S3Utils.clearBucket( s3Client, bucketNameB );
        S3Utils.clearBucket( s3Client, bucketNameC );
        S3Utils.clearBucket( s3Client, bucketNameD );
        existBuckets = s3Client.listBuckets();
        s3Client.createBucket( bucketNameA );
        s3Client.createBucket( bucketNameB );
        s3Client.createBucket( bucketNameC );
        s3Client.createBucket( bucketNameD );
        s3Client.putObject( bucketNameC, objectName, "test" );
        s3Client.putObject( bucketNameD, objectName, "test" );
    }

    @Test
    public void test() {
        Assert.assertTrue( s3Client.doesBucketExistV2( bucketNameA ) );
        Assert.assertTrue( s3Client.doesBucketExistV2( bucketNameB ) );
        Assert.assertTrue( s3Client.doesBucketExistV2( bucketNameC ) );
        Assert.assertTrue( s3Client.doesBucketExistV2( bucketNameD ) );

        // 删除部分桶
        s3Client.deleteBucket( bucketNameB );
        s3Client.deleteObject( bucketNameC, objectName );
        s3Client.deleteBucket( bucketNameC );
        // 校验桶是否删除
        Assert.assertFalse( s3Client.doesBucketExistV2( bucketNameB ) );
        Assert.assertFalse( s3Client.doesBucketExistV2( bucketNameC ) );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            s3Client.deleteBucket( bucketNameA );
            s3Client.deleteObject( bucketNameD, objectName );
            s3Client.deleteBucket( bucketNameD );
            s3Client.shutdown();
        }
    }
}
