package com.sequoiacm.s3.version.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @descreption SCM-4838 :: 并发创建相同桶，设置相同版本控制状态
 * @author Zhaoyujing
 * @Date 2020/7/21
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4838 extends TestScmBase {
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4838";

    @BeforeClass
    private void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket(session, s3WorkSpaces, bucketName);
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        CreateAndSetBucket t1 = new CreateAndSetBucket();
        CreateAndSetBucket t2 = new CreateAndSetBucket();
        te.addWorker(t1);
        te.addWorker(t2);
        te.run();

        if ( 0 == t1.getRetCode() && 0 == t2.getRetCode()) {
            Assert.fail("only one thread can succeed");
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket(session, s3WorkSpaces, bucketName);
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class CreateAndSetBucket extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                ScmBucket bucket = ScmFactory.Bucket.createBucket(ws, bucketName);
                bucket.enableVersionControl();

                ScmBucketVersionStatus bucketStatus = bucket.getVersionStatus();
                Assert.assertEquals(bucketStatus, ScmBucketVersionStatus.Enabled);
            } catch ( ScmException e ) {
                Assert.assertEquals(e.getError().getErrorType(), "BUCKET_EXISTS");
                saveResult( e.getErrorCode(), e );
            }
        }
    }
}
