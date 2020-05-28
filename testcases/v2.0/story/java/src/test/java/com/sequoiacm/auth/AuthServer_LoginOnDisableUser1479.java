package com.sequoiacm.auth;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Description:SCM-1479 :: 用户被禁用
 * @author fanyu
 * @Date:2018年5月18日
 * @version:1.0
 */
public class AuthServer_LoginOnDisableUser1479 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private ScmSession session;
    private String username = "LoginOnDisableUser1479";
    private String passwd = "1479";
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
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        // disable user
        ScmUserModifier modifier = new ScmUserModifier();
        ScmSession session1 = null;
        try {
            // disable user
            modifier.setEnabled( false );
            ScmFactory.User.alterUser( session, user, modifier );
            // login
            session1 = TestScmTools.createSession( site, user.getUsername(),
                    passwd );
            Assert.fail(
                    "expect disable user login fail but act success.session1 "
                            + "= " + session1.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( session1 != null ) {
                session1.close();
            }
        }

        ScmSession session2 = null;
        try {
            // enable user and login
            modifier.setEnabled( true );
            ScmFactory.User.alterUser( session, user, modifier );
            session2 = TestScmTools.createSession( site, username, passwd );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session2 != null ) {
                session2.close();
            }
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
