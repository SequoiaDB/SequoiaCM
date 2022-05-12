package com.sequoiacm.s3.bucket.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-3275:并发创建相同桶
 * @Author YiPan
 * @Date 2021/3/6
 */
public class CreateBucket3275 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket3275";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor( 10000 );
        CreateThread t1 = new CreateThread();
        CreateThread t2 = new CreateThread();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();
        Assert.assertTrue( s3Client.doesBucketExistV2( bucketName ) );
        Assert.assertEquals( t1.result + t2.result, "BucketAlreadyOwnedByYou" );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            S3Utils.clearBucket( s3Client, bucketName );
        }
        s3Client.shutdown();
    }

    class CreateThread extends ResultStore {
        public String result = "";

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3 = S3Utils.buildS3Client();
            try {
                s3.createBucket( bucketName );
            } catch ( AmazonS3Exception e ) {
                result = e.getErrorCode();
            } finally {
                s3.shutdown();
            }
        }
    }
}
