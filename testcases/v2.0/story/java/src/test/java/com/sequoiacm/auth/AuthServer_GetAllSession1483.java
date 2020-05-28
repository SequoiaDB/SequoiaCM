package com.sequoiacm.auth;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSessionInfo;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Description: SCM-1483 :: 查询所有会话
 * @author fanyu
 * @Date:2018年5月18日
 * @version:1.0
 */
public class AuthServer_GetAllSession1483 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private ScmSession session;
    private ScmSession session1;
    private int expNum = 2;
    private String username = "GetAllSession1483";
    private String passwd = "1483";
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
        ScmCursor< ScmSessionInfo > cursor = null;
        try {
            cursor = ScmFactory.Session.listSessions( session );
            int count = 0;
            while ( cursor.hasNext() ) {
                ScmSessionInfo ssInfo = cursor.getNext();
                if ( ssInfo.getSessionId().equals( session1.getSessionId() ) ) {
                    Assert.assertEquals( ssInfo.getSessionId(),
                            session1.getSessionId(), session1.toString() );
                    Assert.assertEquals( ssInfo.getUsername(),
                            session1.getUser(), session1.toString() );
                }
                count++;
            }
            Assert.assertEquals( count >= expNum, true, "" + count );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
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
}
