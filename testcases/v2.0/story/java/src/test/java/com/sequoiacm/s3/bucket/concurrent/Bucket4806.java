package com.sequoiacm.s3.bucket.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
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
 * @descreption SCM-4806 :: 并发创建相同桶，设置相同版本控制状态
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket4806 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4806";
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName);
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor( 10000 );
        CreateBucket t1 = new CreateBucket();
        CreateBucket t2 = new CreateBucket();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();

        if ( t1.getRetCode() != 0 && t2.getRetCode() != 0 ) {
            Assert.assertTrue( false, "all thread failed" );
        }

        BucketVersioningConfiguration configuration = s3Client
                .getBucketVersioningConfiguration( bucketName );
        Assert.assertEquals( configuration.getStatus(), "Enabled" );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private class CreateBucket extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.createBucket( bucketName );
                S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
            } catch ( AmazonS3Exception e ) {
                System.out.println("error:" + e.getErrorCode());
                saveResult( e.getStatusCode(), e );
            } finally {
                if ( s3Client != null ) {
                    s3Client.shutdown();
                }
            }
        }
    }
}
