package com.sequoiacm.s3.bucket.serial;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Descreption SCM-3264:创建桶到达最大数限制（标准模式）
 * @Author YiPan
 * @Date 2021/3/5
 */
public class CreateBucket3264 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket3264";
    private List< Bucket > existBuckets;
    private int bucketNum = 101;

    @BeforeClass
    private void setUp() {
        s3Client = S3Utils.buildS3Client();
        for ( int i = 0; i < bucketNum; i++ ) {
            S3Utils.clearBucket( s3Client, bucketName + i );
        }
        existBuckets = s3Client.listBuckets();
    }

    @Test
    public void test() {
        List< String > expbuckets = new ArrayList<>();
        List< String > actbuckets = new ArrayList<>();
        for ( int i = 0; i < bucketNum; i++ ) {
            expbuckets.add( bucketName + i );
            s3Client.createBucket( bucketName + i );
        }
        // 查看所有桶
        List< Bucket > buckets = s3Client.listBuckets();
        for ( Bucket s : buckets ) {
            actbuckets.add( s.getName() );
        }
        for ( Bucket s : existBuckets ) {
            expbuckets.add( s.getName() );
        }
        Collections.sort( actbuckets );
        Collections.sort( expbuckets );
        Assert.assertEquals( actbuckets, expbuckets );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            for ( int i = 0; i < bucketNum; i++ ) {
                S3Utils.clearBucket( s3Client, bucketName + i );
            }
        }
        s3Client.shutdown();
    }
}
