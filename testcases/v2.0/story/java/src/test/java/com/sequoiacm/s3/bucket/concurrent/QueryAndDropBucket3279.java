package com.sequoiacm.s3.bucket.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-3279:并发获取和删除桶
 * @Author YiPan
 * @Date 2021/3/6
 */
public class QueryAndDropBucket3279 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private String bucketName = "bucket3279";
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        GetBucket t1 = new GetBucket();
        DropBucket t2 = new DropBucket();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();
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

    class GetBucket {

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 amazonS3 = S3Utils.buildS3Client();
            amazonS3.listBuckets();
            amazonS3.shutdown();
        }
    }

    class DropBucket {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 amazonS3 = S3Utils.buildS3Client();
            amazonS3.deleteBucket( bucketName );

        }
    }
}
