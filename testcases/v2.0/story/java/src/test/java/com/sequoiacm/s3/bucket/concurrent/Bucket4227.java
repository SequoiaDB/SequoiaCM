package com.sequoiacm.s3.bucket.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @descreption SCM-4227 :: S3接口和SCM API并发创建桶
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket4227 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private final String bucketName = "bucket4227";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        CreateS3Bucket t1 = new CreateS3Bucket();
        CreateSCMBucket t2 = new CreateSCMBucket();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();

        Assert.assertTrue( s3Client.doesBucketExistV2( bucketName ) );

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
            if ( session != null ) {
                session.close();
            }
        }
    }

    class CreateS3Bucket {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                s3Client.createBucket( bucketName );
            } catch ( AmazonS3Exception e ) {
                Assert.assertEquals( e.getErrorCode(),
                        "BucketAlreadyOwnedByYou" );
            }
        }
    }

    class CreateSCMBucket {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                ScmFactory.Bucket.createBucket( ws, bucketName );
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError().getErrorDescription(),
                        "Bucket already exists" );
            }
        }
    }
}
