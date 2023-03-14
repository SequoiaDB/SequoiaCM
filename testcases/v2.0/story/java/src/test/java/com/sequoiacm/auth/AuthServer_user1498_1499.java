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
 * @FileName SCM-1498:普通角色删除用户 SCM-1499:用户正在被使用，删除该用户
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1498_1499 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_user1498_1499.class );
    private static final String NAME = "auth1498";
    private static final String PASSWORD = NAME;
    private static final int USER_NUM = 2;
    private static SiteWrapper site = null;
    private boolean runSuccess = false;
    private int failTimes = 0;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = ScmSessionUtils.createSession( site );

            // clean new user
            for ( int i = 0; i < USER_NUM; i++ ) {
                try {
                    ScmFactory.User.deleteUser( session, NAME + "_" + i );
                } catch ( ScmException e ) {
                    logger.info( "clean users in setUp, errorMsg = ["
                            + e.getError() + "]" );
                }
            }

            try {
                ScmFactory.Role.deleteRole( session, NAME );
            } catch ( ScmException e ) {
                logger.info( "clean roles in setUp, errorMsg = [" + e.getError()
                        + "]" );
            }

            // create user
            this.createOrdinaryUser();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException, InterruptedException {
        test_delUserByOrdinaryUser();
        test_delUsingUser();
        runSuccess = true;
    }

    private void test_delUserByOrdinaryUser() throws ScmException {
        ScmSession ss = ScmSessionUtils.createSession( site, NAME + "_0",
                PASSWORD );
        try {
            ScmFactory.User.deleteUser( ss, NAME + "_1" );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "delete user by ordinary user, errorMsg = ["
                    + e.getError() + "]" );
        }

        ss.close();
    }

    private void test_delUsingUser() throws ScmException, InterruptedException {
        String username = NAME + "_1";
        List< ScmSession > ss = new ArrayList<>();
        for ( int i = 0; i < 10; i++ ) {
            ScmSession tmpSS = ScmSessionUtils.createSession( site, username,
                    PASSWORD );
            ss.add( tmpSS );
        }
        ScmFactory.User.deleteUser( session, username );

        // check results
        ScmCursor< ScmSessionInfo > cursor = ScmFactory.Session
                .listSessions( session );
        while ( cursor.hasNext() ) {
            ScmSessionInfo ssInfo = cursor.getNext();
            Assert.assertNotEquals( ssInfo.getUsername(), username );

        }
        // SEQUOIACM-793 引入了缓存机制，需要睡眠61s
        Thread.sleep( 61000 );
        for ( ScmSession tmpSS : ss ) {
            try {
                ScmFactory.User.getUser( tmpSS, NAME + "_0" );
                Assert.fail( "expect failed but actual succ." );
            } catch ( ScmException e ) {
                logger.info( "using expired session, errorMsg = ["
                        + e.getError() + "]" );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, NAME + "_0" );
                ScmFactory.Role.deleteRole( session, NAME );
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void createOrdinaryUser() throws ScmException {
        ScmFactory.Role.createRole( session, NAME, "" );

        for ( int i = 0; i < USER_NUM; i++ ) {
            ScmUser scmUser = ScmFactory.User.createUser( session,
                    NAME + "_" + i, ScmUserPasswordType.LOCAL, PASSWORD );

            ScmUserModifier modifier = new ScmUserModifier();
            modifier.addRole( NAME );
            ScmFactory.User.alterUser( session, scmUser, modifier );
        }
    }
}
