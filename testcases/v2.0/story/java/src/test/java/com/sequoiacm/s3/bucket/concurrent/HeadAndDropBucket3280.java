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
 * @descreption SCM-3280:并发head查询和删除相同桶
 * @author YiPan
 * @date 2021/3/6
 * @updateUser YiPan
 * @updateDate 2022/2/18
 * @updateRemark 优化了校验逻辑
 * @version 1.0
 */
public class HeadAndDropBucket3280 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3280";
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor( 10000 );
        GetBucket getBucket = new GetBucket();
        DropBucket dropBucket = new DropBucket();
        te.addWorker( getBucket );
        te.addWorker( dropBucket );
        te.run();
        // 删除必定成功
        Assert.assertFalse( s3Client.doesBucketExistV2( bucketName ) );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            S3Utils.clearBucket( s3Client, bucketName );
        }
        s3Client.shutdown();
    }

    private class GetBucket {

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 amazonS3 = S3Utils.buildS3Client();
            HeadBucketRequest request = new HeadBucketRequest( bucketName );
            try {
                amazonS3.headBucket( request );
            } catch ( AmazonS3Exception e ) {
                // 查询可能出现404 not found异常
                if ( !( e.getErrorCode().contains( "404" ) ) ) {
                    throw e;
                }
            } finally {
                amazonS3.shutdown();
            }
        }
    }

    private class DropBucket {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 amazonS3 = S3Utils.buildS3Client();
            amazonS3.deleteBucket( bucketName );
        }
    }
}
