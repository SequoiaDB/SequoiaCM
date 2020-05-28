package com.sequoiacm.auth.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @FileName SCM-1557:并发修改用户密码并清理会话、删除该部分会话
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1557 extends TestScmBase {
    private static final Logger logger = Logger
            .getLogger( AuthServer_user1557.class );
    private static final String NAME = "auth1555";
    private static final String PASSWORD = NAME;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private List< String > ssIdList = new ArrayList<>();

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
        for ( int i = 0; i < 30; i++ ) {
            ScmSession ss = TestScmTools.createSession( site, NAME, PASSWORD );
            ssIdList.add( ss.getSessionId() );
        }

        AlterUserPasswd alterUser = new AlterUserPasswd();
        CleanSession delRole = new CleanSession();
        Random random = new Random();
        alterUser.start( random.nextInt( 50 ) + 1 );
        delRole.start( random.nextInt( 50 ) + 1 );

        if ( !( alterUser.isSuccess() && delRole.isSuccess() ) ) {
            Assert.fail( alterUser.getErrorMsg() + delRole.getErrorMsg() );
        }

        // check results
        ScmCursor< ScmSessionInfo > cursor = ScmFactory.Session
                .listSessions( session, NAME );
        Assert.assertFalse( cursor.hasNext() );

        try {
            TestScmTools.createSession( site, NAME, PASSWORD );
            Assert.fail( "expect failed but actual succ." );
        } catch ( ScmException e ) {
            logger.info( "login with the old password, errorMsg = ["
                    + e.getError() + "]" );
        }

        ScmSession ss = TestScmTools.createSession( site, NAME,
                "new_" + PASSWORD );
        ss.close();

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Role.deleteRole( session, NAME );
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

    private class AlterUserPasswd extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );

                ScmUser scmUser = ScmFactory.User.getUser( session, NAME );
                ScmUserModifier modifier = new ScmUserModifier();
                modifier.setPassword( PASSWORD, "new_" + PASSWORD );
                modifier.setCleanSessions( true );
                ScmFactory.User.alterUser( session, scmUser, modifier );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class CleanSession extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );

                Random random = new Random();
                int tmpI = random.nextInt( ssIdList.size() );
                ScmFactory.Session.deleteSession( session,
                        ssIdList.get( tmpI ) );
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
