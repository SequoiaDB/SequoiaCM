package com.sequoiacm.auth.concurrent;

import java.util.Random;

import com.sequoiacm.testcommon.listener.GroupTags;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @FileName SCM-1554:并发修改用户属性添加角色、删除该角色
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1554 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_user1554.class );
    private static final String NAME = "auth1554";
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

        ScmFactory.User.createUser( session, NAME, ScmUserPasswordType.LOCAL,
                PASSWORD );
        ScmFactory.Role.createRole( session, NAME, "" );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws ScmException {
        Random random = new Random();

        AlterUserAddRole addRole = new AlterUserAddRole();
        AlterUserDelRole delRole = new AlterUserDelRole();
        addRole.start( random.nextInt( 50 ) + 1 );
        delRole.start( random.nextInt( 50 ) + 1 );

        if ( !( addRole.isSuccess() && delRole.isSuccess() ) ) {
            Assert.fail( addRole.getErrorMsg() + delRole.getErrorMsg() );
        }

        // check results
        ScmFactory.Role.deleteRole( session, NAME );
        ScmFactory.User.deleteUser( session, NAME );

        try {
            ScmFactory.User.getUser( session, NAME );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "delete not exist user, errorMsg = [" + e.getError()
                    + "]" );
        }

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
            }
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private class AlterUserAddRole extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );

                ScmUser scmUser = ScmFactory.User.getUser( session, NAME );
                ScmUserModifier modifier = new ScmUserModifier();
                modifier.addRole( NAME );
                ScmFactory.User.alterUser( session, scmUser, modifier );
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
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
