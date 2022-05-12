package com.sequoiacm.s3.bucket.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @Descreption SCM-3276:并发查询桶列表信息
 * @Author YiPan
 * @Date 2021/3/6
 */
public class QueryBucket3276 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket3276";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void test() throws Exception {
        String expResult = s3Client.listBuckets().toString();
        ThreadExecutor te = new ThreadExecutor( 10000 );
        CreateThread t1 = new CreateThread();
        CreateThread t2 = new CreateThread();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            S3Utils.clearBucket( s3Client, bucketName );
        }
        s3Client.shutdown();
    }

    class CreateThread {

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3 = S3Utils.buildS3Client();
            List< Bucket > buckets = s3.listBuckets();
            boolean flag = false;
            for ( Bucket s : buckets ) {
                if ( s.getName().equals( bucketName ) ) {
                    flag = true;
                }
            }
            Assert.assertTrue( flag );
            s3.shutdown();
        }
    }
}
