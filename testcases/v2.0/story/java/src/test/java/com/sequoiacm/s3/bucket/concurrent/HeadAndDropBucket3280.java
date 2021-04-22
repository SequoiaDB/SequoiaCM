package com.sequoiacm.s3.bucket.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-3280:并发head查询和删除相同桶
 * @Author YiPan
 * @Date 2021/3/6
 */
public class HeadAndDropBucket3280 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3280";
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor( 10000 );
        GetBucket t1 = new GetBucket();
        DropBucket t2 = new DropBucket();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();
        // a.查询成功，删除成功 b.查询失败，删除成功
        if ( ( t1.result + t2.result ).equals( "" )
                || ( t1.result + t2.result ).equals( "404" ) ) {
            Assert.assertFalse( s3Client.doesBucketExistV2( bucketName ) );
        } else {
            Assert.fail( "t1:" + t1.result + "t2:" + t2.result );
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

    class GetBucket {
        public String result = "";

        @ExecuteOrder(step = 1)
        public void run() {
            AmazonS3 amazonS3 = S3Utils.buildS3Client();
            HeadBucketRequest request = new HeadBucketRequest( bucketName );
            try {
                amazonS3.headBucket( request );
            } catch ( AmazonS3Exception e ) {
                result = e.getErrorCode();
            } finally {
                amazonS3.shutdown();
            }
        }
    }

    class DropBucket {
        public String result = "";

        @ExecuteOrder(step = 1)
        public void run() {
            try {
                AmazonS3 amazonS3 = S3Utils.buildS3Client();
                amazonS3.deleteBucket( bucketName );
            } catch ( AmazonS3Exception e ) {
                result = e.getErrorCode();
            }
        }
    }
}
