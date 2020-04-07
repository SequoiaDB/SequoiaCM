package com.sequoiacm.auth.concurrent;

import java.util.Random;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
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
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @FileName SCM-1556:并发删除用户、删除角色，角色被该用户所拥有
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1556 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_user1556.class );
    private static final String NAME = "auth1556";
    private static final String PASSWORD = NAME;
    private boolean runSuccess = false;
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
        try {
            ScmFactory.Role.deleteRole( session, NAME );
        } catch ( ScmException e ) {
            logger.info(
                    "clean roles in setUp, errorMsg = [" + e.getError() + "]" );
        }

        this.createUserAndAddRole();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        Random random = new Random();

        DelUser delUser = new DelUser();
        DelRole delRole = new DelRole();
        delUser.start( random.nextInt( 50 ) + 1 );
        delRole.start( random.nextInt( 50 ) + 1 );

        if ( !( delUser.isSuccess() && delRole.isSuccess() ) ) {
            Assert.fail( delUser.getErrorMsg() + delRole.getErrorMsg() );
        }

        // check results
        ScmCursor< ScmUser > userCursor = ScmFactory.User.listUsers( session );
        while ( userCursor.hasNext() ) {
            String roleName = userCursor.getNext().getUsername();
            Assert.assertNotEquals( roleName, NAME );
        }
        try {
            ScmFactory.User.getUser( session, NAME );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info(
                    "get not exist user, errorMsg = [" + e.getError() + "]" );
        }

        ScmCursor< ScmRole > roleCursor = ScmFactory.Role.listRoles( session );
        while ( roleCursor.hasNext() ) {
            String roleName = roleCursor.getNext().getRoleName();
            Assert.assertNotEquals( roleName, "ROLE_" + NAME );
        }
        try {
            ScmFactory.Role.getRole( session, NAME );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info(
                    "get not exist role, errorMsg = [" + e.getError() + "]" );
        }

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void createUserAndAddRole() throws ScmException {
        ScmFactory.User.createUser( session, NAME, ScmUserPasswordType.LOCAL,
                PASSWORD );
        ScmFactory.Role.createRole( session, NAME, "" );

        ScmUser scmUser = ScmFactory.User.getUser( session, NAME );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( NAME );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }

    private class DelUser extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmFactory.User.deleteUser( session, NAME );
            } catch ( ScmException e ) {
                if ( ScmError.HTTP_NOT_FOUND != e.getError() ) {
                    e.printStackTrace();
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DelRole extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmFactory.Role.deleteRole( session, NAME );
            } catch ( ScmException e ) {
                if ( ScmError.HTTP_NOT_FOUND != e.getError() ) {
                    e.printStackTrace();
                    throw e;
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
