package com.sequoiacm.s3.bucket.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * @Descreption SCM-4231:SCM API查询桶和S3接口删除桶并发
 * @Author YiPan
 * @CreateDate 2022/5/16
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4231 extends TestScmBase {
    private final String bucketName = "bucket4241";
    private AmazonS3 s3Client;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws Exception {
        // 创建桶
        s3Client.createBucket( bucketName );

        // 并发查询和删除桶
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new ScmGetBucket() );
        te.addWorker( new S3DeleteBucket() );
        te.run();

        Assert.assertFalse( s3Client.doesBucketExistV2( bucketName ) );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private class ScmGetBucket {

        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmSession session = ScmSessionUtils
                    .createSession( ScmInfo.getRootSite() );
            try {
                for ( int i = 0; i < 20; i++ ) {
                    ScmFactory.Bucket.getBucket( session, bucketName );
                }
            } catch ( ScmException e ) {
                if ( !e.getError().equals( ScmError.BUCKET_NOT_EXISTS ) ) {
                    throw e;
                }
            } finally {
                session.close();
            }
        }
    }

    private class S3DeleteBucket {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.deleteBucket( bucketName );
            } finally {
                s3Client.shutdown();
            }
        }
    }
}