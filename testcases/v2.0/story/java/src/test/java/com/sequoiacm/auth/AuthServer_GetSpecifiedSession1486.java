package com.sequoiacm.auth;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
import com.sequoiacm.testcommon.ScmSessionUtils;

/**
 * @Description: SCM-1486:查询指定会话，不返回用户详情
 * @author fanyu
 * @Date:2018年5月18日
 * @version:1.0
 */
public class AuthServer_GetSpecifiedSession1486 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private ScmSession session;
    private ScmSession session1;
    private String username = "GetSpecifiedSession1485";
    private String passwd = "1485";
    private ScmUser user;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = ScmSessionUtils.createSession( site );
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
            session1 = ScmSessionUtils.createSession( site, username, passwd );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        try {
            ScmSessionInfo ssInfo = ScmFactory.Session.getSessionInfo( session,
                    session1.getSessionId() );
            Assert.assertEquals( ssInfo.getSessionId(), session1.getSessionId(),
                    session1.toString() );
            Assert.assertEquals( ssInfo.getUsername(), session1.getUser(),
                    session1.toString() );
            Assert.assertNotNull( ssInfo.getCreationTime() );
            Assert.assertNotNull( ssInfo.getLastAccessedTime() );
            Assert.assertEquals(
                    ssInfo.getCreationTime() <= ssInfo.getLastAccessedTime(),
                    true );
            Assert.assertNotNull( ssInfo.getMaxInactiveInterval() );
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
