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
 * @Descreption SCM-3267:桶中有对象，删除桶（标准模式） SCM-3268:删除空桶（标准模式）
 * @Author YiPan
 * @Date 2021/3/5
 */
public class DropBucket3267_3268 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket3267";
    private String objectName = "object3267";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() {
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, objectName, "test" );

        // 删除未清空的桶
        try {
            s3Client.deleteBucket( bucketName );
            Assert.fail( "expect fail but success" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "BucketNotEmpty" );
        }
        boolean flag = false;
        List< Bucket > buckets = s3Client.listBuckets();
        for ( Bucket s : buckets ) {
            if ( s.getName().equals( bucketName ) ) {
                flag = true;
            }
        }
        Assert.assertTrue( flag );
        // 清空桶
        s3Client.deleteObject( bucketName, objectName );
        // 删除空桶
        s3Client.deleteBucket( bucketName );
        // 检查桶不存在
        buckets = s3Client.listBuckets();
        for ( Bucket s : buckets ) {
            if ( s.getName().equals( bucketName ) ) {
                flag = false;
            }
        }
        Assert.assertTrue( flag );
        // 再次删除
        try {
            s3Client.deleteBucket( bucketName );
            Assert.fail( "expect fail but success" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchBucket" );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            S3Utils.clearBucket( s3Client, bucketName );
        }
        s3Client.shutdown();
    }
}
