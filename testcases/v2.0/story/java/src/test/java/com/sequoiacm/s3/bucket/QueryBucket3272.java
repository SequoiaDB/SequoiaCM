package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-3272:doesBucketExist查询桶（标准模式）
 * @Author YiPan
 * @Date 2021/3/8
 */
public class QueryBucket3272 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket3272";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws Exception {
        // 创建桶查询
        s3Client.createBucket( bucketName );
        if ( !( s3Client.doesBucketExistV2( bucketName ) ) ) {
            throw new Exception( bucketName + "is not exist" );
        }
        // 删除桶
        s3Client.deleteBucket( bucketName );
        if ( s3Client.doesBucketExistV2( bucketName ) ) {
            throw new Exception( bucketName + "is exist" );
        }
        // 再次创建桶查询
        s3Client.createBucket( bucketName );
        if ( !( s3Client.doesBucketExistV2( bucketName ) ) ) {
            throw new Exception( bucketName + "is not exist" );
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
