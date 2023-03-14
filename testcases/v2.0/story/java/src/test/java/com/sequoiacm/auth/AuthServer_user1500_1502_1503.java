package com.sequoiacm.auth;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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
import com.sequoiacm.testcommon.ScmSessionUtils;

/**
 * @FileName SCM-1500:修改admin角色的用户密码，password_type为LOCAL，并清理该用户会话
 *           SCM-1502:修改非admin角色的用户密码，该用户password_type为LOCAL，老密码错误
 *           SCM-1503:修改password_type为LOCAL的用户密码正确，不清理该用户下会话
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1500_1502_1503 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_user1500_1502_1503.class );
    private static final String NAME = "auth1500";
    private static final String PASSWORD = NAME;
    private static final int USER_NUM = 2;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmRole authAdminRole = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );

        // clean new user
        for ( int i = 0; i < USER_NUM; i++ ) {
            try {
                ScmFactory.User.deleteUser( session, NAME + "_" + i );
            } catch ( ScmException e ) {
                logger.info( "clean users in setUp, errorMsg = [" + e.getError()
                        + "]" );
            }
        }

        try {
            ScmFactory.Role.deleteRole( session, NAME );
        } catch ( ScmException e ) {
            logger.info(
                    "clean roles in setUp, errorMsg = [" + e.getError() + "]" );
        }

        // get AUTH_ADMIN role
        ScmUser adminUser = ScmFactory.User.getUser( session,
                TestScmBase.scmUserName );
        authAdminRole = adminUser.getRoles().iterator().next();

        this.createUser();
    }

    @Test
    private void test() throws ScmException, InterruptedException {
        test_alterPasswd01();
        test_alterPasswd02();
        test_alterPasswdForOrdinaryUser();
        runSuccess = true;
    }

    /*
     * the same new and old password
     */
    private void test_alterPasswd01() throws ScmException {
        String username = NAME + "_0";
        ScmUser scmUser = ScmFactory.User.getUser( session, username );

        ScmUserModifier modifier = new ScmUserModifier();
        modifier.setPassword( PASSWORD, PASSWORD );
        ScmFactory.User.alterUser( session, scmUser, modifier );

        // check results
        ScmSession newSS = ScmSessionUtils.createSession( site, username,
                PASSWORD );
        newSS.close();
    }

    /*
     * different new and old password
     */
    private void test_alterPasswd02() throws ScmException, InterruptedException {
        String username = NAME + "_0";
        ScmUser scmUser = ScmFactory.User.getUser( session, username );

        // ready sessions for clean sessions
        List< ScmSession > ss = new ArrayList<>();
        for ( int i = 0; i < 10; i++ ) {
            ScmSession tmpSS = ScmSessionUtils.createSession( site, username,
                    PASSWORD );
            ss.add( tmpSS );
        }

        // alter password and clean ssessions
        String newPassword = PASSWORD + "_new";
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.setPassword( PASSWORD, newPassword );
        modifier.setCleanSessions( true );
        ScmFactory.User.alterUser( session, scmUser, modifier );

        // check results for alter password
        try {
            ScmSessionUtils.createSession( site, username, PASSWORD );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "login with the old password, errorMsg = ["
                    + e.getError() + "]" );
        }

        ScmSession newSS = ScmSessionUtils.createSession( site, username,
                newPassword );
        newSS.close();

        // check results for clean sessions
        ScmCursor< ScmSessionInfo > cursor = ScmFactory.Session
                .listSessions( session, username );
        Assert.assertFalse( cursor.hasNext() );
        // SEQUOIACM-793 引入了缓存机制，需要睡眠61s
        Thread.sleep( 61000 );
        for ( ScmSession tmpSS : ss ) {
            try {
                ScmFactory.User.getUser( tmpSS, username );
                Assert.fail( "expect failed but actual succ." );
            } catch ( ScmException e ) {
                logger.info( "using expired session, errorMsg = ["
                        + e.getError() + "]" );
            }
        }

        // recover the old password
        modifier = new ScmUserModifier();
        modifier.setPassword( newPassword, PASSWORD );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }

    /*
     * alter ordinary user, and don't clean
     */
    private void test_alterPasswdForOrdinaryUser() throws ScmException {
        String username = NAME + "_1";
        ScmUser scmUser = ScmFactory.User.getUser( session, username );

        // ready sessions for clean sessions
        List< ScmSession > ss = new ArrayList<>();
        int ssNum = 10;
        for ( int i = 0; i < ssNum; i++ ) {
            ScmSession tmpSS = ScmSessionUtils.createSession( site, username,
                    PASSWORD );
            ss.add( tmpSS );
        }

        // alter password and clean ssessions
        String newPassword = PASSWORD + "_new";
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.setPassword( "", newPassword );
        modifier.setCleanSessions( false );
        ScmFactory.User.alterUser( session, scmUser, modifier );

        // check results for alter password
        try {
            ScmSessionUtils.createSession( site, username, PASSWORD );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "login with the old password, errorMsg = ["
                    + e.getError() + "]" );
        }

        ScmSession newSS = ScmSessionUtils.createSession( site, username,
                newPassword );
        newSS.close();

        // check results for clean sessions
        ScmCursor< ScmSessionInfo > cursor = ScmFactory.Session
                .listSessions( session, username );
        int actSSNum = 0;
        while ( cursor.hasNext() ) {
            actSSNum++;
            cursor.getNext();
        }
        Assert.assertEquals( actSSNum, ssNum );
        for ( ScmSession tmpSS : ss ) {
            ScmFactory.User.getUser( tmpSS, username );
            tmpSS.close();
        }

        // recover the old password
        modifier = new ScmUserModifier();
        modifier.setPassword( "", PASSWORD );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < USER_NUM; i++ ) {
                    ScmFactory.User.deleteUser( session, NAME + "_" + i );
                }
                ScmFactory.Role.deleteRole( session, NAME );
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void createUser() throws ScmException {
        // create admin user
        ScmUser scmUser = ScmFactory.User.createUser( session, NAME + "_0",
                ScmUserPasswordType.LOCAL, PASSWORD );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( authAdminRole );
        ScmFactory.User.alterUser( session, scmUser, modifier );

        // create ordinary user
        scmUser = ScmFactory.User.createUser( session, NAME + "_1",
                ScmUserPasswordType.LOCAL, PASSWORD );
        ScmFactory.Role.createRole( session, NAME, "" );
        modifier = new ScmUserModifier();
        modifier.addRole( NAME );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }
}
