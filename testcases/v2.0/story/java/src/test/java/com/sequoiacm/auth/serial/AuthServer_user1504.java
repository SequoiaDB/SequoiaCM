package com.sequoiacm.auth.serial;

import com.sequoiacm.testcommon.scmutils.ConfUtil;
import org.apache.log4j.Logger;
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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @FileName SCM-1504:修改password_type为LDAP的用户密码
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1504 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_user1504.class );
    private static final String PASSWORD = "auth1504";
    private static SiteWrapper site = null;
    private boolean runSuccess = false;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        ConfUtil.checkLDAPConfig();
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        // clean new user
        try {
            ScmFactory.User.deleteUser( session, TestScmBase.ldapUserName );
        } catch ( ScmException e ) {
            logger.info(
                    "clean users in setUp, errorMsg = [" + e.getError() + "]" );
        }
        ScmFactory.User.createUser( session, TestScmBase.ldapUserName,
                ScmUserPasswordType.LDAP, "aaa" );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException, InterruptedException {
        test_setLdapPasswd();
        test_setToLocalNoPasswd();
        test_setLocalPasswd();
        runSuccess = true;
    }

    // svn: 4408
    private void test_setLdapPasswd()
            throws ScmException, InterruptedException {
        ScmUser scmUser = ScmFactory.User.getUser( session,
                TestScmBase.ldapUserName );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.setPassword( "", "test" );
        try {
            ScmFactory.User.alterUser( session, scmUser, modifier );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "ldap password can not modify, errorMsg = ["
                    + e.getError() + "]" );
        }
        // check results
        scmUser = ScmFactory.User.getUser( session, TestScmBase.ldapUserName );
        Assert.assertEquals( scmUser.getUsername(), TestScmBase.ldapUserName );
        Assert.assertEquals( scmUser.getPasswordType(),
                ScmUserPasswordType.LDAP );
        Assert.assertTrue( scmUser.isEnabled() );
        ScmSession ss = TestScmTools.createSession( site,
                TestScmBase.ldapUserName, TestScmBase.ldapPassword );
        ss.isClosed();
        ss.close();

        try {
            TestScmTools.createSession( site, TestScmBase.ldapUserName,
                    "test" );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "password error, errorMsg = [" + e.getError() + "]" );
        }
    }

    private void test_setToLocalNoPasswd()
            throws ScmException, InterruptedException {
        ScmUser scmUser = ScmFactory.User.getUser( session,
                TestScmBase.ldapUserName );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.setPasswordType( ScmUserPasswordType.LOCAL );
        try {
            ScmFactory.User.alterUser( session, scmUser, modifier );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            // e.printStackTrace();
            logger.info( "set to local user but not set password, errorMsg = ["
                    + e.getError() + "]" );
        }

        // check results
        scmUser = ScmFactory.User.getUser( session, TestScmBase.ldapUserName );
        Assert.assertEquals( scmUser.getPasswordType(),
                ScmUserPasswordType.LDAP );
        ScmSession ss = TestScmTools.createSession( site,
                TestScmBase.ldapUserName, TestScmBase.ldapPassword );
        ss.isClosed();
        ss.close();
    }

    private void test_setLocalPasswd()
            throws ScmException, InterruptedException {
        ScmUser scmUser = ScmFactory.User.getUser( session,
                TestScmBase.ldapUserName );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.setPasswordType( ScmUserPasswordType.LOCAL );
        modifier.setPassword( "", PASSWORD );
        ScmFactory.User.alterUser( session, scmUser, modifier );
        // check results
        scmUser = ScmFactory.User.getUser( session, TestScmBase.ldapUserName );
        Assert.assertEquals( scmUser.getUsername(), TestScmBase.ldapUserName );
        Assert.assertEquals( scmUser.getPasswordType(),
                ScmUserPasswordType.LOCAL );
        Assert.assertTrue( scmUser.isEnabled() );

        ScmSession ss = TestScmTools.createSession( site,
                TestScmBase.ldapUserName, PASSWORD );
        ss.isClosed();
        ss.close();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, TestScmBase.ldapUserName );
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }
}
