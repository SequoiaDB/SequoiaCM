package com.sequoiacm.s3.bucket.serial;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-4235:S3接口列取、统计桶，SCM API并发创建、删除桶
 * @Author YiPan
 * @CreateDate 2022/5/13
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4235 extends TestScmBase {
    private final String bucketNameBase = "bucket4235no";
    private List< String > bucketNames = new ArrayList<>();
    private int baseBucketNum = 15;
    private int addBucketNum = 10;
    private int deleteBucketNum = 5;
    private ScmSession session;
    private AmazonS3 s3Client;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        clearBuckets();
    }

    @Test
    public void test() throws Exception {
        // 创建桶
        for ( int i = 0; i < baseBucketNum; i++ ) {
            s3Client.createBucket( bucketNameBase + i );
            bucketNames.add( bucketNameBase + i );
        }

        // 查询桶和删除桶并发
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new ScmDeleteBucket() );
        te.addWorker( new ScmCreateBucket() );
        te.addWorker( new S3ListBuckets() );
        te.run();

        // 校验并发结果
        List< Bucket > buckets = s3Client.listBuckets();
        List< String > actBucketNames = new ArrayList<>();
        for ( Bucket bucket : buckets ) {
            actBucketNames.add( bucket.getName() );
        }
        Assert.assertEqualsNoOrder( actBucketNames.toArray(),
                bucketNames.toArray() );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                clearBuckets();
            }
        } finally {
            session.close();
            s3Client.shutdown();
        }
    }

    private class ScmDeleteBucket {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools
                    .createSession( ScmInfo.getRootSite() );
            try {
                for ( int i = baseBucketNum
                        - deleteBucketNum; i < deleteBucketNum; i++ ) {
                    ScmFactory.Bucket.deleteBucket( session,
                            bucketNameBase + i );
                    bucketNames.remove( bucketNameBase + i );
                }
            } finally {
                session.close();
            }
        }
    }

    private class ScmCreateBucket {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools
                    .createSession( ScmInfo.getRootSite() );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces,
                    session );
            try {
                for ( int i = baseBucketNum; i < baseBucketNum
                        + addBucketNum; i++ ) {
                    ScmFactory.Bucket.createBucket( ws, bucketNameBase + i );
                    bucketNames.add( bucketNameBase + i );
                }
            } finally {
                session.close();
            }
        }
    }

    private class S3ListBuckets {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                List< Bucket > buckets = s3Client.listBuckets();
                Assert.assertNotEquals( buckets.size(), 0 );
            } finally {
                s3Client.shutdown();
            }
        }
    }

    private void clearBuckets() throws ScmException {
        ScmCursor< ScmBucket > scmBucketScmCursor = ScmFactory.Bucket
                .listBucket( session, s3WorkSpaces, TestScmBase.scmUserName );
        while ( scmBucketScmCursor.hasNext() ) {
            String bucketName = scmBucketScmCursor.getNext().getName();
            if ( bucketName.contains( bucketNameBase ) ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        }
    }
}