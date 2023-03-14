package com.sequoiacm.s3.bucket.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @descreption SCM-4228 :: S3接口和SCM API并发删除桶
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket4228 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4228";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        DeleteS3Bucket t1 = new DeleteS3Bucket();
        DeleteSCMBucket t2 = new DeleteSCMBucket();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();

        Assert.assertFalse( s3Client.doesBucketExistV2( bucketName ) );

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

    class DeleteS3Bucket {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                s3Client.deleteBucket( bucketName );
            } catch ( AmazonS3Exception e ) {
                Assert.assertEquals( e.getErrorCode(), "NoSuchBucket" );
            }
        }
    }

    class DeleteSCMBucket {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                ScmFactory.Bucket.deleteBucket( session, bucketName );
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError().getErrorDescription(), "Bucket is not exist" );
            }
        }
    }

}
