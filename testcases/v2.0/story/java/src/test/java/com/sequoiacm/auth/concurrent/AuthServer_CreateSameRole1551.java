package com.sequoiacm.auth.concurrent;

import java.util.Collection;
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
 * @Description: SCM-1551 :: 并发创建相同角色
 * @author fanyu
 * @Date:2018年5月21日
 * @version:1.0
 */
public class AuthServer_CreateSameRole1551 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private ScmSession session;
    private String username = "CreateSameUser1545";
    private String roleName = "CreateSameUser1545_r";
    private String passwd = "1545";
    private ScmUser user;
    private AtomicInteger atom = new AtomicInteger( 0 );

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
            site = ScmInfo.getSite();
            try {
                ScmFactory.Role.deleteRole( session, roleName );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
            try {
                ScmFactory.User.deleteUser( session, username );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        try {
            ScmUserModifier modifier = new ScmUserModifier();
            ScmUser superUser = ScmFactory.User
                    .getUser( session, TestScmBase.scmUserName );
            Collection< ScmRole > superRoles = superUser.getRoles();
            modifier.addRoles( superRoles );
            user = ScmFactory.User
                    .createUser( session, username, ScmUserPasswordType.LOCAL,
                            passwd );
            ScmFactory.User.alterUser( session, user, modifier );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        CreateRole cThread = new CreateRole();
        cThread.start( 3 );
        boolean cflag = cThread.isSuccess();
        Assert.assertEquals( cflag, true, cThread.getErrorMsg() );
        Assert.assertEquals( atom.get(), 1, atom.get() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Role.deleteRole( session, roleName );
                ScmFactory.User.deleteUser( session, username );
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

    private class CreateRole extends TestThreadBase {
        @Override
        public void exec() {
            try {
                ScmRole role = ScmFactory.Role
                        .createRole( session, roleName, null );
                atom.getAndIncrement();
                check( role );
            } catch ( ScmException e ) {
                e.printStackTrace();
                if ( e.getError() != ScmError.HTTP_BAD_REQUEST &&
                        e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                    Assert.fail( e.getMessage() );
                }
            }
        }

        private void check( ScmRole role ) {
            try {
                ScmRole actRole = ScmFactory.Role.getRole( session, roleName );
                Assert.assertEquals( actRole.getDescription(),
                        role.getDescription() );
                Assert.assertEquals( actRole.getRoleId(), role.getRoleId() );
                Assert.assertEquals( actRole.getRoleName(),
                        role.getRoleName() );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }
}
