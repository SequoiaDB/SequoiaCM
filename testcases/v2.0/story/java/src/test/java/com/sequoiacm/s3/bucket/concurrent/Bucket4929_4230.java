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
 * @Descreption SCM-4929:getBucekt(session, bucketId)接口互通特性测试
 *              SCM-4930:S3接口删除桶和SCM API根据bucketId获取桶并发
 * @Author YiPan
 * @CreateDate 2022/5/16
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4929_4230 extends TestScmBase {
    private final String bucketName = "bucket4929";
    private AmazonS3 s3Client;
    private ScmSession session;
    private long buckeId;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        session = TestScmTools.createSession();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws Exception {
        // 创建桶,根据BucketId获取
        s3Client.createBucket( bucketName );
        buckeId = ScmFactory.Bucket.getBucket( session, bucketName ).getId();
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, buckeId );
        Assert.assertEquals( bucket.getName(), bucketName );

        // 并发获取和删除桶
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
            session.close();
            s3Client.shutdown();
        }
    }

    private class ScmGetBucket {

        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmSession session = TestScmTools
                    .createSession( ScmInfo.getRootSite() );
            try {
                for ( int i = 0; i < 20; i++ ) {
                    ScmFactory.Bucket.getBucket( session, buckeId );
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