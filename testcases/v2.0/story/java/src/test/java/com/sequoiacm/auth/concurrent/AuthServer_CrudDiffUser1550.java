package com.sequoiacm.auth.concurrent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sequoiacm.testcommon.scmutils.ConfUtil;
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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @Description:SCM-1550 :: 并发增删改查不同用户
 * @author fanyu
 * @Date:2018年5月21日
 * @version:1.0
 */
public class AuthServer_CrudDiffUser1550 extends TestScmBase {
    private boolean runSuccess;
    private SiteWrapper site;
    private ScmSession session;
    private int userNum = 3;
    private String username = "CrudDiffUser1550";
    private String passwd = "1550";
    private List< ScmUser > userList = new CopyOnWriteArrayList< ScmUser >();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        ConfUtil.checkLDAPConfig();
        try {
            site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
            site = ScmInfo.getSite();
            for ( int i = 0; i < userNum; i++ ) {
                ScmFactory.User.deleteUser( session, username + "_" + i );
            }
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            for ( int i = 0; i < userNum; i++ ) {
                ScmUser user = ScmFactory.User.createUser( session,
                        username + "_" + i, ScmUserPasswordType.LOCAL, passwd );
                userList.add( user );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        CreateUser cThread = new CreateUser();
        DeleteUser dThread = new DeleteUser();
        QueryUser qThread = new QueryUser();
        ModifierUser mThread = new ModifierUser();
        cThread.start();
        mThread.start();
        qThread.start();
        dThread.start();
        boolean cflag = cThread.isSuccess();
        boolean mflag = mThread.isSuccess();
        boolean qflag = qThread.isSuccess();
        boolean dflag = dThread.isSuccess();
        Assert.assertEquals( dflag, true, dThread.getErrorMsg() );
        Assert.assertEquals( qflag, true, qThread.getErrorMsg() );
        Assert.assertEquals( mflag, true, mThread.getErrorMsg() );
        Assert.assertEquals( cflag, true, cThread.getErrorMsg() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 1; i < userNum + 1; i++ ) {
                    ScmFactory.User.deleteUser( session, userList.get( i ) );
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class CreateUser extends TestThreadBase {
        @Override
        public void exec() {
            try {
                String username1 = username + "_" + UUID.randomUUID();
                ScmUser user = ScmFactory.User.createUser( session, username1,
                        ScmUserPasswordType.LOCAL, passwd );
                userList.add( user );
                check( username1, user );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        private void check( String username, ScmUser user ) {
            ScmUser actUser;
            try {
                actUser = ScmFactory.User.getUser( session, username );
                Assert.assertEquals( actUser.getRoles(), user.getRoles() );
                Assert.assertEquals( actUser.getUserId(), user.getUserId() );
                Assert.assertEquals( actUser.getUsername(),
                        user.getUsername() );
                Assert.assertEquals( actUser.getPasswordType(),
                        user.getPasswordType() );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private class DeleteUser extends TestThreadBase {
        @Override
        public void exec() {
            try {
                ScmFactory.User.deleteUser( session, userList.get( 0 ) );
                check( userList.get( 0 ) );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        private void check( ScmUser user ) {
            try {
                ScmFactory.User.getUser( session, user.getUsername() );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private class QueryUser extends TestThreadBase {
        @Override
        public void exec() {
            try {
                ScmUser actuser = ScmFactory.User.getUser( session,
                        userList.get( 1 ).getUsername() );
                check( actuser, userList.get( 1 ) );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        private void check( ScmUser actuser, ScmUser user ) {
            Assert.assertEquals( actuser.getRoles(), user.getRoles() );
            Assert.assertEquals( actuser.getUserId(), user.getUserId() );
            Assert.assertEquals( actuser.getUsername(), user.getUsername() );
            Assert.assertEquals( actuser.getPasswordType(),
                    user.getPasswordType() );
        }
    }

    private class ModifierUser extends TestThreadBase {
        @Override
        public void exec() {
            try {
                ScmUserModifier modifier = new ScmUserModifier();
                modifier.setPasswordType( ScmUserPasswordType.LDAP );
                ScmUser actUser = ScmFactory.User.alterUser( session,
                        userList.get( 2 ), modifier );
                check( actUser, userList.get( 2 ) );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        private void check( ScmUser actuser, ScmUser user ) {
            Assert.assertEquals( actuser.getRoles(), user.getRoles() );
            Assert.assertEquals( actuser.getUserId(), user.getUserId() );
            Assert.assertEquals( actuser.getUsername(), user.getUsername() );
            Assert.assertEquals( actuser.getPasswordType(),
                    ScmUserPasswordType.LDAP );
        }
    }
}
