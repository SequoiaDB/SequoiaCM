package com.sequoiacm.auth;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
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
 * @FileName SCM-1509:查询所有用户 SCM-1511:指定filter条件过滤查询 SCM-1513:指定的角色不存在，查询该角色下的用户
 *           SCM-1516:查询和删除不存在的用户
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1509_to_1516 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_user1509_to_1516.class );
    private static final String NAME = "auth1509";
    private static final String PASSWORD = NAME;
    private static final int USER_NUM = 3;
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
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_listUserByFilter() throws ScmException {
        BSONObject cond = ScmQueryBuilder.start( "password_type" ).is( "LOCAL" )
                .and( "enabled" ).is( false ).and( "has_role" ).is( NAME )
                .get();

        ScmCursor< ScmUser > cursor = ScmFactory.User.listUsers( session,
                cond );
        ArrayList< ScmUser > rtUsers = new ArrayList< ScmUser >();
        while ( cursor.hasNext() ) {
            ScmUser user = cursor.getNext();
            rtUsers.add( user );
            System.out.println( user.getUsername() );
        }
        Assert.assertEquals( rtUsers.size(), 1 );
        Assert.assertEquals( rtUsers.get( 0 ).getUsername(), NAME + "_2" );

        runSuccess = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_listUserByExistRole() throws ScmException {
        BSONObject cond = ScmQueryBuilder.start( "has_role" ).is( "test111A" )
                .get();
        ScmCursor< ScmUser > cursor = null;
        try {
            cursor = ScmFactory.User.listUsers( session, cond );
            Assert.assertEquals( cursor.hasNext(), false );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
        runSuccess = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_delNotExistUser() throws ScmException {
        try {
            ScmFactory.User.deleteUser( session, "test" );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "delete not exist user, errorMsg = [" + e.getError()
                    + "]" );
        }

        runSuccess = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test_getNotExistUser() throws ScmException {
        try {
            ScmFactory.User.getUser( session, "test" );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info(
                    "get not exist user, errorMsg = [" + e.getError() + "]" );
        }

        runSuccess = true;
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
        // create role
        ScmRole role = ScmFactory.Role.createRole( session, NAME, "" );

        // create admin user
        ScmUser scmUser = ScmFactory.User.createUser( session, NAME + "_0",
                ScmUserPasswordType.LOCAL, PASSWORD );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( authAdminRole );
        ScmFactory.User.alterUser( session, scmUser, modifier );

        // create ordinary user
        scmUser = ScmFactory.User.createUser( session, NAME + "_1",
                ScmUserPasswordType.LOCAL, PASSWORD );
        modifier = new ScmUserModifier();
        modifier.setEnabled( false );
        ScmFactory.User.alterUser( session, scmUser, modifier );

        scmUser = ScmFactory.User.createUser( session, NAME + "_2",
                ScmUserPasswordType.LOCAL, PASSWORD );
        modifier = new ScmUserModifier();
        modifier.addRole( role );
        modifier.setEnabled( false );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }

}
