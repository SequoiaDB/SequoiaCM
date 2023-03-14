package com.sequoiacm.auth.concurrent;

import java.util.Collection;
import java.util.Random;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @FileName SCM-1555:并发修改用户属性删除角色、删除该角色
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1555 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_user1555.class );
    private static final String NAME = "auth1555";
    private static final String PASSWORD = NAME;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );

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

        AlterUserDelRole alterUser = new AlterUserDelRole();
        DelRole delRole = new DelRole();
        alterUser.start( random.nextInt( 50 ) + 1 );
        delRole.start( random.nextInt( 50 ) + 1 );

        if ( !( alterUser.isSuccess() && delRole.isSuccess() ) ) {
            Assert.fail( alterUser.getErrorMsg() + delRole.getErrorMsg() );
        }

        // check results
        ScmUser scmUser = ScmFactory.User.getUser( session, NAME );
        Collection< ScmRole > roles = scmUser.getRoles();
        Assert.assertEquals( roles.size(), 0 );

        try {
            ScmFactory.Role.deleteRole( session, NAME );
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

    private void createUserAndAddRole() throws ScmException {
        ScmFactory.User.createUser( session, NAME, ScmUserPasswordType.LOCAL,
                PASSWORD );
        ScmFactory.Role.createRole( session, NAME, "" );

        ScmUser scmUser = ScmFactory.User.getUser( session, NAME );
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.addRole( NAME );
        ScmFactory.User.alterUser( session, scmUser, modifier );
    }

    private class DelRole extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmFactory.Role.deleteRole( session, NAME );
            } catch ( ScmException e ) {
                if ( ScmError.HTTP_NOT_FOUND != e.getError()
                        && ScmError.HTTP_FORBIDDEN != e.getError()
                        && ScmError.HTTP_INTERNAL_SERVER_ERROR != e
                                .getError() ) {
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

    private class AlterUserDelRole extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );

                ScmUser scmUser = ScmFactory.User.getUser( session, NAME );
                ScmUserModifier modifier = new ScmUserModifier();
                modifier.delRole( NAME );
                ScmFactory.User.alterUser( session, scmUser, modifier );
            } catch ( ScmException e ) {
                if ( ScmError.HTTP_INTERNAL_SERVER_ERROR != e.getError() ) {
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
