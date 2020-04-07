package com.sequoiacm.auth;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSessionInfo;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @FileName SCM-1505:禁用其他用户，清理当前用户下会话
 * 			  SCM-1506:禁用其他用户，不清理当前用户下会话
 * 			  SCM-1507:禁用当前用户
 * 			  SCM-1508:重复禁用
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1505_to_1508 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_user1505_to_1508.class );
    private static final String NAME = "auth1505";
    private static final String PASSWORD = NAME;
    private boolean runSuccess = false;
    private int failTimes = 0;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmRole authAdminRole = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );

        // clean new user
        try {
            ScmFactory.User.deleteUser( session, NAME );
        } catch ( ScmException e ) {
            logger.info(
                    "clean users in setUp, errorMsg = [" + e.getError() + "]" );
        }

        // get AUTH_ADMIN role
        ScmUser adminUser = ScmFactory.User
                .getUser( session, TestScmBase.scmUserName );
        authAdminRole = adminUser.getRoles().iterator().next();

        this.createUser();
    }

    @BeforeMethod
    private void initMethod() {
        if ( !runSuccess ) {
            failTimes++;
        }
        runSuccess = false;
    }

    @AfterMethod
    private void teardownMethod() throws ScmException {
        if ( failTimes > 1 ) {
            runSuccess = false;
        }

        // recover the user
        ScmUser scmUser = ScmFactory.User.getUser( session, NAME );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.setEnabled( true );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }

    /*
     * disabled user, and clean sessions
     */
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_disabledUser01() throws ScmException {
        // ready sessions for clean sessions
        ScmUser scmUser = ScmFactory.User.getUser( session, NAME );
        List< ScmSession > ss = new ArrayList<>();
        int ssNum = 10;
        for ( int i = 0; i < ssNum; i++ ) {
            ScmSession tmpSS = TestScmTools
                    .createSession( site, NAME, PASSWORD );
            ss.add( tmpSS );
        }

        // disabled the user and clean ssessions
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.setEnabled( false );
        modifier.setCleanSessions( true );
        ScmFactory.User.alterUser( session, scmUser, modifier );

        // check results for enabled
        scmUser = ScmFactory.User.getUser( session, NAME );
        Assert.assertFalse( scmUser.isEnabled() );
        try {
            TestScmTools.createSession( site, NAME, PASSWORD );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info(
                    "login but the user disabled, errorMsg = [" + e.getError() +
                            "]" );
        }

        // check results for clean sessions
        ScmCursor< ScmSessionInfo > cursor = ScmFactory.Session
                .listSessions( session, NAME );
        Assert.assertFalse( cursor.hasNext() );
        for ( ScmSession tmpSS : ss ) {
            try {
                ScmFactory.User.getUser( tmpSS, NAME );
                Assert.fail( "expect failed but actual succ." );
            } catch ( ScmException e ) {
                logger.info(
                        "using expired session, errorMsg = [" + e.getError() +
                                "]" );
            }
        }

        runSuccess = true;
    }

    /*
     * disabled user, and don't clean sessions
     */
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_disabledUser02() throws ScmException {
        ScmUser scmUser = ScmFactory.User.getUser( session, NAME );

        // ready sessions for clean sessions
        List< ScmSession > ss = new ArrayList<>();
        int ssNum = 10;
        for ( int i = 0; i < ssNum; i++ ) {
            ScmSession tmpSS = TestScmTools
                    .createSession( site, NAME, PASSWORD );
            ss.add( tmpSS );
        }

        // disabled the user and clean ssessions
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.setEnabled( false );
        modifier.setCleanSessions( false );
        ScmFactory.User.alterUser( session, scmUser, modifier );

        // check results for enabled
        scmUser = ScmFactory.User.getUser( session, NAME );
        Assert.assertFalse( scmUser.isEnabled() );
        try {
            TestScmTools.createSession( site, NAME, PASSWORD );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info(
                    "login but the user disabled, errorMsg = [" + e.getError() +
                            "]" );
        }

        // check results for clean sessions
        ScmCursor< ScmSessionInfo > cursor = ScmFactory.Session
                .listSessions( session, NAME );
        int actSSNum = 0;
        while ( cursor.hasNext() ) {
            actSSNum++;
            cursor.getNext();
        }
        Assert.assertEquals( actSSNum, ssNum );
        for ( ScmSession tmpSS : ss ) {
            ScmFactory.User.getUser( tmpSS, NAME );
            tmpSS.close();
        }

        runSuccess = true;
    }

    /*
     * repeat disabled the user
     */
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_disabledCurrentUser() throws ScmException {
        ScmSession ss = TestScmTools.createSession( site, NAME, PASSWORD );

        ScmUser scmUser = ScmFactory.User.getUser( ss, NAME );
        // disabled the user
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.setEnabled( false );
        try {
            ScmFactory.User.alterUser( ss, scmUser, modifier );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "disabled current user, errorMsg = [" + e.getError() +
                    "]" );
        }

        // check results
        scmUser = ScmFactory.User.getUser( ss, NAME );
        Assert.assertTrue( scmUser.isEnabled() );
        Assert.assertFalse( ss.isClosed() );
        ss.close();

        TestScmTools.createSession( site, NAME, PASSWORD );
        ss.close();

        runSuccess = true;
    }

    /*
     * repeat disabled the user
     */
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_repeatDisabledUser() throws ScmException {
        ScmUser scmUser = ScmFactory.User.getUser( session, NAME );
        // disabled the user
        for ( int i = 0; i < 10; i++ ) {
            ScmUserModifier modifier = new ScmUserModifier();
            modifier.setEnabled( false );
            ScmFactory.User.alterUser( session, scmUser, modifier );
        }

        // check results
        scmUser = ScmFactory.User.getUser( session, NAME );
        Assert.assertFalse( scmUser.isEnabled() );

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, NAME );
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void createUser() throws ScmException {
        ScmUser scmUser = ScmFactory.User
                .createUser( session, NAME, ScmUserPasswordType.LOCAL,
                        PASSWORD );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( authAdminRole );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }

}
