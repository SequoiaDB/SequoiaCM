package com.sequoiacm.auth.concurrent;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequoiacm.client.exception.ScmException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmAccesskeyInfo;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5794:管理员用户并发刷新普通用户accesskey和secretkey
 * @author YiPan
 * @date 2023/1/7
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class RefreshAccessKey5794 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private final String username = "user5794";
    private final String password = "pwd5794";
    private final String roleName = "role5794";

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        ScmUser user = ScmAuthUtils.createUser( session, username, password );
        ScmRole role = ScmAuthUtils.createRole( session, roleName );
        ScmAuthUtils.alterUser( session, ws.getName(), user, role,
                ScmPrivilegeType.ALL );
        ScmAccesskeyInfo scmAccesskeyInfo = ScmFactory.S3
                .refreshAccesskey( session, username, password );
        String[] keys = { scmAccesskeyInfo.getAccesskey(),
                scmAccesskeyInfo.getSecretkey() };
        ScmAuthUtils.checkPriorityByS3( keys, ws.getName() );
    }

    @Test
    public void test() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        RefreshKey t1 = new RefreshKey();
        RefreshKey t2 = new RefreshKey();
        te.addWorker( t1 );
        te.addWorker( t2 );
        te.run();

        // 校验结果，只有一个线程的key是可用的
        boolean result = checkS3AccessKey( t1.getScmAccesskeyInfo() );
        if ( result ) {
            Assert.assertFalse( checkS3AccessKey( t2.getScmAccesskeyInfo() ) );
        } else {
            Assert.assertTrue( checkS3AccessKey( t2.getScmAccesskeyInfo() ) );
        }

        runSuccess = true;
    }

    private boolean checkS3AccessKey( ScmAccesskeyInfo scmAccesskeyInfo )
            throws Exception {
        AmazonS3 s3 = S3Utils.buildS3Client( scmAccesskeyInfo.getAccesskey(),
                scmAccesskeyInfo.getSecretkey() );
        try {
            s3.listBuckets();
            return true;
        } catch ( AmazonS3Exception e ) {
            if ( e.getErrorCode().equals( "InvalidAccessKeyId" ) ) {
                return false;
            } else {
                throw e;
            }
        } finally {
            s3.shutdown();
        }
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ScmAuthUtils.deleteUser( session, username );
                ScmFactory.Role.deleteRole( session, roleName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class RefreshKey {
        private ScmAccesskeyInfo scmAccesskeyInfo;

        public ScmAccesskeyInfo getScmAccesskeyInfo() {
            return scmAccesskeyInfo;
        }

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            scmAccesskeyInfo = ScmFactory.S3.refreshAccesskey( session,
                    username, password );
        }
    }
}
