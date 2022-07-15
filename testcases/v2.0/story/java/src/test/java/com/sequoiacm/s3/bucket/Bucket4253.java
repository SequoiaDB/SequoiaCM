package com.sequoiacm.s3.bucket;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @descreption SCM-4253 :: 使用SCM API删除不存在桶
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket4253 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws_4253";
    private ScmWorkspace ws_test = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4253";

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        int siteNum = ScmInfo.getSiteNum();

        ws_test = ScmWorkspaceUtil.createS3WS( session, wsName );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
    }

    @Test
    public void test() throws ScmException, IOException {
        ScmFactory.Bucket.createBucket( ws_test, bucketName );

        ScmFactory.Bucket.deleteBucket( session, bucketName );

        try {
            ScmFactory.Bucket.deleteBucket( session, bucketName );
            Assert.fail( "Delete bucket：" + bucketName + " should failed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError().getErrorType(),
                    "BUCKET_NOT_EXISTS" );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                ScmWorkspaceUtil.deleteWs( wsName, session );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
