package com.sequoiacm.auth.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @Description: SCM-1546 :: 并发修改相同用户不同属性
 * @author fanyu
 * @Date:2018年5月21日
 * @version:1.0
 */
public class AuthServer_ModifySameUserDiffAttr1546 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private ScmSession session;
    private String username = "ModifySameUserDiffAttr1546";
    private String passwd = "1546";
    private String newPasswd = "1546_1";
    private ScmUser user;
    private List< ScmUserModifier > modiferList = new CopyOnWriteArrayList< ScmUserModifier >();
    private AtomicInteger atom = new AtomicInteger( 0 );

    @BeforeClass(alwaysRun = true)
    private void setUp() {
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
            ScmUserModifier modifier = new ScmUserModifier();
            ScmUser superUser = ScmFactory.User.getUser( session,
                    TestScmBase.scmUserName );
            Collection< ScmRole > superRoles = superUser.getRoles();
            modifier.addRoles( superRoles );
            user = ScmFactory.User.createUser( session, username,
                    ScmUserPasswordType.LOCAL, passwd );
            ScmFactory.User.alterUser( session, user, modifier );
            for ( int i = 0; i < 3; i++ ) {
                ScmUserModifier modifier1 = new ScmUserModifier();
                if ( i % 2 == 0 ) {
                    modifier1.delRoles( superRoles );
                    modifier1.setPasswordType( ScmUserPasswordType.LDAP );
                    modifier1.setCleanSessions( true );
                } else {
                    modifier1.setEnabled( false );
                    modifier1.addRoles( superRoles );
                    modifier1.setPassword( passwd, newPasswd );
                }
                modiferList.add( modifier );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        ModifyAttr mThread = new ModifyAttr();
        mThread.start( 3 );
        boolean mflag = mThread.isSuccess();
        Assert.assertEquals( mflag, true, mThread.getErrorMsg() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, user );
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

    private class ModifyAttr extends TestThreadBase {
        @Override
        public void exec() {
            try {
                ScmUserModifier modifier = modiferList
                        .get( atom.getAndIncrement() );
                ScmUser user1 = ScmFactory.User.alterUser( session, user,
                        modifier );
                check( user1, modifier );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        private void check( ScmUser user1, ScmUserModifier modifier ) {
            Assert.assertEquals( user1.getRoles().isEmpty(),
                    modifier.getAddRoles().isEmpty() );
            Assert.assertEquals( user1.getUserId(), user.getUserId() );
            if ( modifier.getPasswordType() == null ) {
                Assert.assertEquals( user1.getPasswordType(),
                        ScmUserPasswordType.LOCAL );
            } else {
                Assert.assertEquals( user1.getPasswordType(),
                        modifier.getPasswordType() );
            }
        }
    }
}
