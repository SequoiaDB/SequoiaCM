package com.sequoiacm.s3.bucket.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-4230:S3接口查询桶和SCM API删除桶并发
 * @Author YiPan
 * @CreateDate 2022/5/13
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4230 extends TestScmBase {
    private final String bucketName = "bucket4230";
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

        // 查询桶和删除桶并发
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new ScmDeleteBucket() );
        te.addWorker( new S3GetBucket() );
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

    private class ScmDeleteBucket {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools
                    .createSession( ScmInfo.getRootSite() );
            try {
                ScmFactory.Bucket.deleteBucket( session, bucketName );
            } finally {
                session.close();
            }
        }
    }

    private class S3GetBucket {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.getBucketAcl( bucketName );
            } catch ( AmazonS3Exception e ) {
                if ( !( e.getErrorCode().equals( "NoSuchBucket" ) ) ) {
                    throw e;
                }
            } finally {
                s3Client.shutdown();
            }
        }
    }
}