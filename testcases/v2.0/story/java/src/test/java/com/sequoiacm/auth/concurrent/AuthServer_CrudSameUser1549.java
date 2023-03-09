package com.sequoiacm.auth.concurrent;

import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @Description: SCM-1549 :: 并发修改、查询、删除相同用户
 * @author fanyu
 * @Date:2018年5月21日
 * @version:1.0
 */
public class AuthServer_CrudSameUser1549 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession session;
    private String username = "CrudSameUser1549";
    private String passwd = "1549";
    private ScmUser user;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        ConfUtil.checkLDAPConfig();
        try {
            site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
            site = ScmInfo.getSite();
            ScmFactory.User.deleteUser( session, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            user = ScmFactory.User.createUser( session, username,
                    ScmUserPasswordType.LOCAL, passwd );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void test() {
        DeleteSameUser dThread = new DeleteSameUser();
        QuerySameUser qThread = new QuerySameUser();
        ModifierSameUser mThread = new ModifierSameUser();
        mThread.start();
        qThread.start();
        dThread.start();
        boolean mflag = mThread.isSuccess();
        boolean qflag = qThread.isSuccess();
        boolean dflag = dThread.isSuccess();
        Assert.assertEquals( dflag, true, dThread.getErrorMsg() );
        Assert.assertEquals( qflag, true, qThread.getErrorMsg() );
        Assert.assertEquals( mflag, true, mThread.getErrorMsg() );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
    }

    private class DeleteSameUser extends TestThreadBase {
        @Override
        public void exec() {
            try {
                ScmFactory.User.deleteUser( session, user );
                check( user );
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

    private class QuerySameUser extends TestThreadBase {
        @Override
        public void exec() {
            try {
                ScmUser actuser = ScmFactory.User.getUser( session,
                        user.getUsername() );
                check( actuser, user );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }

        private void check( ScmUser actuser, ScmUser user ) {
            Assert.assertEquals( actuser.getRoles(), user.getRoles() );
            Assert.assertEquals( actuser.getUserId(), user.getUserId() );
            Assert.assertEquals( actuser.getUsername(), user.getUsername() );
            Assert.assertEquals( actuser.getPasswordType()
                    .equals( ScmUserPasswordType.LOCAL )
                    || actuser.getPasswordType()
                            .equals( ScmUserPasswordType.LDAP ),
                    true );
        }
    }

    private class ModifierSameUser extends TestThreadBase {
        @Override
        public void exec() {
            try {
                ScmUserModifier modifier = new ScmUserModifier();
                modifier.setPasswordType( ScmUserPasswordType.LDAP );
                ScmUser actUser = ScmFactory.User.alterUser( session, user,
                        modifier );
                check( actUser, user );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }

        private void check( ScmUser actuser, ScmUser user ) {
            Assert.assertEquals( actuser.getRoles(), user.getRoles() );
            Assert.assertEquals( actuser.getUserId(), user.getUserId() );
            Assert.assertEquals( actuser.getUsername(), user.getUsername() );
            Assert.assertEquals( actuser.getPasswordType()
                    .equals( ScmUserPasswordType.LOCAL )
                    || actuser.getPasswordType()
                            .equals( ScmUserPasswordType.LDAP ),
                    true );
        }
    }
}
