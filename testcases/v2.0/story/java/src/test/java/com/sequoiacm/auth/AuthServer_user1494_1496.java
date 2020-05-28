package com.sequoiacm.auth;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @FileName SCM-1494:admin角色删除当前用户 SCM-1496:admin角色删除admin用户（非最后一个）
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1494_1496 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_user1494_1496.class );
    private static final String NAME = "auth1493";
    private static final String PASSWORD = NAME;
    private static final int USER_NUM = 2;
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
        for ( int i = 0; i < USER_NUM; i++ ) {
            try {
                ScmFactory.User.deleteUser( session, NAME + "_" + i );
            } catch ( ScmException e ) {
                logger.info( "clean users in setUp, errorMsg = [" + e.getError()
                        + "]" );
            }
        }

        // get AUTH_ADMIN role
        ScmUser adminUser = ScmFactory.User.getUser( session,
                TestScmBase.scmUserName );
        authAdminRole = adminUser.getRoles().iterator().next();
        System.out.println( authAdminRole.getRoleId() );

        // ready users
        this.createUsers();
    }

    @BeforeMethod
    private void initMethod() {
        if ( !runSuccess ) {
            failTimes++;
        }
        runSuccess = false;
    }

    @AfterMethod
    private void teardownMethod() {
        if ( failTimes > 1 ) {
            runSuccess = false;
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_deleteCurrentUser() throws ScmException {
        ScmSession ss = TestScmTools.createSession( site, NAME + "_0",
                PASSWORD );
        try {
            ScmFactory.User.deleteUser( ss, NAME + "_0" );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info(
                    "delete Current user, errorMsg = [" + e.getError() + "]" );
        }
        ss.close();

        runSuccess = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_deleteAdminUser() throws ScmException {
        ScmSession ss = TestScmTools.createSession( site, NAME + "_0",
                PASSWORD );
        // delete admin user
        ScmFactory.User.deleteUser( ss, NAME + "_1" );
        try {
            ScmFactory.User.getUser( session, NAME + "_1" );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "get user after delete, errorMsg = [" + e.getError()
                    + "]" );
        }
        ss.close();

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            ScmFactory.User.deleteUser( session, NAME + "_0" );
            if ( runSuccess || TestScmBase.forceClear ) {
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void createUsers() throws ScmException {
        // create user, and add AUTH_ADMIN role
        for ( int i = 0; i < USER_NUM; i++ ) {
            // create ScmUser
            ScmUser scmUser = ScmFactory.User.createUser( session,
                    NAME + "_" + i, ScmUserPasswordType.LOCAL, PASSWORD );
            // add AUTH_ADMIN role
            ScmUserModifier modifier = new ScmUserModifier();
            modifier.addRole( authAdminRole );
            ScmFactory.User.alterUser( session, scmUser, modifier );
        }
    }

}
