package com.sequoiacm.net.auth.serial;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
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
 * @FileName SCM- SCM-1489:指定认证方式创建LOCAL用户
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1489 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_user1489.class );
    private static final String NAME = "auth1489";
    private static SiteWrapper site = null;
    private boolean runSuccess = false;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );

        // clean new user
        try {
            ScmFactory.User.deleteUser( session, TestScmBase.ldapUserName );
        } catch ( ScmException e ) {
            logger.info(
                    "clean users in setUp, errorMsg = [" + e.getError() + "]" );
        }
        try {
            ScmFactory.User.deleteUser( session, NAME );
        } catch ( ScmException e ) {
            logger.info(
                    "clean users in setUp, errorMsg = [" + e.getError() + "]" );
        }
        try {
            ScmFactory.Role.deleteRole( session, NAME );
        } catch ( ScmException e ) {
            logger.info(
                    "clean roles in setUp, errorMsg = [" + e.getError() + "]" );
        }

        // create user and role
        ScmFactory.Role.createRole( session, NAME, "" );

        ScmUser scmUser = ScmFactory.User.createUser( session, NAME,
                ScmUserPasswordType.LOCAL, "aa" );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( NAME );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }

    /*
     * @BeforeMethod private void initMethod() { if (!runSuccess) { failTimes++;
     * } runSuccess = false; }
     * @AfterMethod private void afterMethod() { if (failTimes > 1) { runSuccess
     * = false; } }
     */

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException, InterruptedException {
        test_createUser();
        test_listUser();
        test_deleteUser();
        runSuccess = true;
    }

    private void test_createUser() throws ScmException, InterruptedException {
        // create user and role
        ScmUser scmUser = ScmFactory.User.createUser( session,
                TestScmBase.ldapUserName, ScmUserPasswordType.LDAP, "" );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( NAME );
        ScmFactory.User.alterUser( session, scmUser, modifier );

        // check results
        ScmSession ss = TestScmTools.createSession( site,
                TestScmBase.ldapUserName, TestScmBase.ldapPassword );
        ss.isClosed();
        ss.close();

        scmUser = ScmFactory.User.getUser( session, TestScmBase.ldapUserName );
        Assert.assertEquals( scmUser.getUsername(), TestScmBase.ldapUserName );
        Assert.assertEquals( scmUser.getPasswordType(),
                ScmUserPasswordType.LDAP );
        Assert.assertTrue( scmUser.isEnabled() );
        Assert.assertTrue( scmUser.hasRole( NAME ) );
        runSuccess = true;
    }

    private void test_listUser() throws ScmException {
        BSONObject filter = ScmQueryBuilder.start().and( "password_type" )
                .is( ScmUserPasswordType.LDAP ).and( "has_role" ).is( NAME )
                .get();
        ScmCursor< ScmUser > cursor = ScmFactory.User.listUsers( session,
                filter );
        int userNum = 0;
        while ( cursor.hasNext() ) {
            userNum++;
            ScmUser user = cursor.getNext();
            System.out.println( "user1 = " + user.toString() );
            Assert.assertEquals( user.getPasswordType(),
                    ScmUserPasswordType.LDAP );
        }
        Assert.assertEquals( userNum, 1 );

        runSuccess = true;
    }

    private void test_deleteUser() throws ScmException {
        ScmUser user = ScmFactory.User.getUser( session,
                TestScmBase.ldapUserName );
        System.out.println( "user2 = " + user.toString() );
        ScmFactory.Role.deleteRole( session, NAME );
        ScmFactory.User.deleteUser( session, TestScmBase.ldapUserName );

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
}
