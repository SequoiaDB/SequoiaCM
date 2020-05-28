package com.sequoiacm.auth;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
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
 * @FileName SCM-1519:普通角色创建角色 SCM-1523:admin角色删除普通角色，且被删除的角色已被用户所拥有
 *           SCM-1524:普通角色删除角色
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_role1519_1523_1524 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_role1519_1523_1524.class );
    private static final String NAME = "auth1519";
    private static final String PASSWORD = NAME;
    private static final int ROLE_NUM = 2;
    private boolean runSuccess = false;
    private int failTimes = 0;
    private SiteWrapper site = null;
    private ScmSession session = null;

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
        for ( int i = 0; i < ROLE_NUM + 1; i++ ) {
            try {
                ScmFactory.Role.deleteRole( session, NAME + "_" + i );
            } catch ( ScmException e ) {
                logger.info( "clean roles in setUp, errorMsg = [" + e.getError()
                        + "]" );
            }
        }

        // create user and role, then add ordinary role
        this.createOrdinaryUser();
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
    private void test_createRoleByOrdinaryUser() throws ScmException {
        ScmSession ss = TestScmTools.createSession( site, NAME, PASSWORD );
        try {
            ScmFactory.Role.createRole( ss, NAME + "_2", "" );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "create role by ordinary user, errorMsg = ["
                    + e.getError() + "]" );
        }

        ss.close();
        runSuccess = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_delUsingRole() throws ScmException {
        try {
            ScmFactory.Role.deleteRole( session, NAME + "_0" );
            ScmFactory.Role.getRole( session, NAME + "_0" );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "get role after delete, errorMsg = [" + e.getError()
                    + "]" );
        }

        runSuccess = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_delRoleByOrdinaryUser() throws ScmException {
        ScmSession ss = TestScmTools.createSession( site, NAME, PASSWORD );
        try {
            ScmFactory.Role.deleteRole( ss, NAME + "_1" );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "delete role by ordinary user, errorMsg = ["
                    + e.getError() + "]" );
        }

        ss.close();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, NAME );
            }
            ScmFactory.Role.deleteRole( session, NAME + "_1" );
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void createOrdinaryUser() throws ScmException {
        // create user
        ScmUser scmUser = ScmFactory.User.createUser( session, NAME,
                ScmUserPasswordType.LOCAL, PASSWORD );
        // create role
        for ( int i = 0; i < ROLE_NUM; i++ ) {
            ScmFactory.Role.createRole( session, NAME + "_" + i, "test" );
        }

        // add ordinary role
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( NAME + "_0" );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }

}
