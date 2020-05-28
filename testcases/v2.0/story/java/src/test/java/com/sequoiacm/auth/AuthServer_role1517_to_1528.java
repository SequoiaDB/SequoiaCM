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
 * @FileName SCM-1517:admin角色创建角色，角色名以“ROLE_”开头 ............
 *           SCM-1528:查询/删除不存在的角色
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_role1517_to_1528 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_role1517_to_1528.class );
    private static final String NAME = "auth1517";
    private static final String PASSWORD = NAME;
    private static final int ROLE_NUM = 2;
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
        for ( int i = 0; i < ROLE_NUM; i++ ) {
            try {
                ScmFactory.Role.deleteRole( session, NAME + "_" + i );
            } catch ( ScmException e ) {
                logger.info( "clean roles in setUp, errorMsg = [" + e.getError()
                        + "]" );
            }
        }

        // get AUTH_ADMIN role
        ScmUser adminUser = ScmFactory.User.getUser( session,
                TestScmBase.scmUserName );
        authAdminRole = adminUser.getRoles().iterator().next();

        // create user, add AUTH_ADMIN role
        this.createAdminUser();
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
    private void test_CDQRole() throws ScmException {
        ScmSession ss = TestScmTools.createSession( site, NAME, PASSWORD );
        String roleName = NAME + "_0";

        // create role
        ScmFactory.Role.createRole( session, roleName, "" );

        // get role
        ScmRole scmRole = ScmFactory.Role.getRole( ss, roleName );
        Assert.assertEquals( scmRole.getRoleName(), "ROLE_" + roleName );

        // delete role
        ScmFactory.Role.deleteRole( ss, roleName );
        try {
            ScmFactory.Role.getRole( ss, roleName );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "get user after delete, errorMsg = [" + e.getError()
                    + "]" );
        }

        ss.close();
        runSuccess = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_CDQRoleByROLE_() throws ScmException {
        ScmSession ss = TestScmTools.createSession( site, NAME, PASSWORD );
        String roleName = "ROLE_" + NAME + "_1";

        // create role
        ScmFactory.Role.createRole( session, roleName, "" );

        // get role
        ScmRole scmRole = ScmFactory.Role.getRole( ss, roleName );
        Assert.assertEquals( scmRole.getRoleName(), roleName );

        // delete role
        ScmFactory.Role.deleteRole( ss, roleName );
        try {
            ScmFactory.Role.getRole( ss, roleName );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "get user after delete, errorMsg = [" + e.getError()
                    + "]" );
        }

        ss.close();
        runSuccess = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_repeatCreateRole() throws ScmException {
        String roleName = NAME + "_2";
        ScmFactory.Role.createRole( session, roleName, "new_role" );
        try {
            ScmFactory.Role.createRole( session, roleName, "repeat_role" );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info(
                    "repeat create role, errorMsg = [" + e.getError() + "]" );
        }

        // delete role
        ScmFactory.Role.deleteRole( session, roleName );

        runSuccess = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_getAndDelNotExistRole() throws ScmException {
        String roleName = NAME + "_3";
        try {
            ScmFactory.Role.getRole( session, roleName );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info(
                    "get not exist role, errorMsg = [" + e.getError() + "]" );
        }

        try {
            ScmFactory.Role.deleteRole( session, roleName );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "delete not exist role, errorMsg = [" + e.getError()
                    + "]" );
        }

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

    private void createAdminUser() throws ScmException {
        // create user
        ScmUser scmUser = ScmFactory.User.createUser( session, NAME,
                ScmUserPasswordType.LOCAL, PASSWORD );
        // add AUTH_ADMIN role
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( authAdminRole );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }
}
