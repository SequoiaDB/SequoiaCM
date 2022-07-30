package com.sequoiacm.s3.bucket;

import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-4735 :: 不同用户设置/获取桶版本控制状态
 * @author wuyan
 * @Date 2022.07.07
 * @version 1.00
 */
public class Bucket4735 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession sessionA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsA = null;
    private final String username = "user4735";
    private final String password = "passwd4735";
    private String bucketName = "bucket4735";

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        sessionA = TestScmTools.createSession( site );
        S3Utils.clearBucket( sessionA, bucketName );
        ScmAuthUtils.createUser( sessionA, username, password );
        sessionB = TestScmTools.createSession( ScmInfo.getSite(), username,
                password );
        wsA = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, sessionA );
    }

    @Test
    public void test() throws ScmException {
        ScmBucket bucketInfo1 = ScmFactory.Bucket.createBucket( wsA,
                bucketName );
        bucketInfo1.enableVersionControl();
        try {
            ScmBucket bucketInfo = ScmFactory.Bucket.getBucket( sessionB,
                    bucketName );
            bucketInfo.suspendVersionControl();
            Assert.fail( "get bucket should be failed by other user!" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorType(), "OPERATION_UNAUTHORIZED",
                    "errorMsg: " + e.getMessage() + ", errorCode="
                            + e.getError() );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Bucket.deleteBucket( sessionA, bucketName );
                ScmFactory.User.deleteUser( sessionA, username );
            }
        } finally {
            sessionA.close();
            sessionB.close();
        }
    }
}
