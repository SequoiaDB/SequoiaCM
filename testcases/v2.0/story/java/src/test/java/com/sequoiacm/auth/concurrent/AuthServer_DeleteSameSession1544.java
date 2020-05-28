package com.sequoiacm.auth.concurrent;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @Description:SCM-1544 :: 并发删除相同会话
 * @author fanyu
 * @Date:2018年5月19日
 * @version:1.0
 */
public class AuthServer_DeleteSameSession1544 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private ScmSession session;
    private ScmSession session1;
    private String username = "DeleteSameSession1544";
    private String passwd = "1544";
    private ScmUser user;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
            site = ScmInfo.getSite();
            ScmFactory.User.deleteUser( session, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            user = ScmFactory.User.createUser( session, username,
                    ScmUserPasswordType.LOCAL, passwd );
            session1 = TestScmTools.createSession( site, username, passwd );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {

        DeleteSameSession dThread = new DeleteSameSession();
        dThread.start( 30 );
        boolean dflag = dThread.isSuccess();
        Assert.assertEquals( dflag, true, dThread.getErrorMsg() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, user );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class DeleteSameSession extends TestThreadBase {
        @Override
        public void exec() {
            try {
                ScmFactory.Session.deleteSession( session,
                        session1.getSessionId() );
                ScmFactory.Session.getSessionInfo( session,
                        session1.getSessionId() );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND
                        && e.getError() != ScmError.HTTP_BAD_REQUEST ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }
}
