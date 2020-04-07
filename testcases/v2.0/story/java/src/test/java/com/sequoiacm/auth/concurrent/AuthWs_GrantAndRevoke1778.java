package com.sequoiacm.auth.concurrent;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmPrivilege;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @author fanyu
 * @Description:SCM-1778 :: 授权和取消授权并发
 * @Date:2018年6月15日
 * @version:1.0
 */
public class AuthWs_GrantAndRevoke1778 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private ScmWorkspace wsA;
    private String username = "AuthWs_GrantAndRevoke1778";
    private String rolename = "1778_R";
    private String passwd = "1778";
    private ScmUser user;
    private ScmRole role;
    private String dirpath = "/AuthWs_GrantAndRevoke1778";
    private ScmResource rs;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            site = ScmInfo.getRootSite();
            wsp = ScmInfo.getWs();
            sessionA = TestScmTools.createSession( site );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
            cleanEnv();
            prepare();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        GrantRs gThread = new GrantRs( rs, role );
        RevokeRs rThread = new RevokeRs( rs, role );
        gThread.start();
        rThread.start();

        boolean g1Flag = gThread.isSuccess();
        boolean rFlag = rThread.isSuccess();

        Assert.assertEquals( g1Flag, true, gThread.getErrorMsg() );
        Assert.assertEquals( rFlag, true, rThread.getErrorMsg() );

        ScmCursor< ScmPrivilege > cursor = ScmFactory.Privilege
                .listPrivilegesByResource( sessionA, rs );
        while ( cursor.hasNext() ) {
            System.out.println( "cursor = " + cursor.getNext().toString() );
        }

        ScmCursor< ScmResource > cursor2 = ScmFactory.Resource
                .listResourceByWorkspace( sessionA, wsp.getName() );
        while ( cursor.hasNext() ) {
            System.out.println( "cursor2 = " + cursor2.getNext().toString() );
        }

        grantPriAndAttachRole( sessionA, rs, user, role,
                ScmPrivilegeType.READ );
        grantPriAndAttachRole( sessionA, rs, user, role,
                ScmPrivilegeType.CREATE );
        ScmAuthUtils.checkPriority( site, username, passwd, role, wsp );
        check( dirpath );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Role.revokePrivilege( sessionA, role, rs,
                        ScmPrivilegeType.READ );
                ScmFactory.Role.revokePrivilege( sessionA, role, rs,
                        ScmPrivilegeType.CREATE );
                ScmFactory.Role.deleteRole( sessionA, rolename );
                ScmFactory.User.deleteUser( sessionA, user );
                ScmFactory.Directory.deleteInstance( wsA, dirpath );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void grantPriAndAttachRole( ScmSession session, ScmResource rs,
            ScmUser user, ScmRole role,
            ScmPrivilegeType privileges ) throws ScmException {
        ScmUserModifier modifier = new ScmUserModifier();
        ScmFactory.Role.grantPrivilege( session, role, rs, privileges );
        modifier.addRole( role );
        ScmFactory.User.alterUser( session, user, modifier );
    }

    private void check( String dirpath ) throws ScmException {
        ScmSession session = null;
        String subname = "1778";
        String subpath = dirpath + "/" + subname + "/";
        ScmWorkspace ws = null;
        ScmDirectory subdir = null;
        try {
            session = TestScmTools.createSession( site, username, passwd );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            ScmDirectory dir = ScmFactory.Directory.getInstance( ws, dirpath );
            Assert.assertEquals( dir.getPath(), dirpath + "/" );

            dir.createSubdirectory( subname );

            subdir = ScmFactory.Directory.getInstance( ws, subpath );
            Assert.assertEquals( subdir.getName(), subname );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        try {
            ScmFactory.Directory.deleteInstance( ws, subpath );
            Assert.fail(
                    "the user does not have priority to delete subpath = " +
                            subpath );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( subdir != null ) {
                ScmFactory.Directory.deleteInstance( wsA, subpath );
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void cleanEnv() {
        try {
            ScmFactory.Role.deleteRole( sessionA, rolename );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            ScmFactory.User.deleteUser( sessionA, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        try {
            ScmFactory.Directory.deleteInstance( wsA, dirpath );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private void prepare() throws Exception {
        rs = ScmResourceFactory
                .createDirectoryResource( wsp.getName(), dirpath );
        ScmFactory.Directory.createInstance( wsA, dirpath );

        user = ScmFactory.User
                .createUser( sessionA, username, ScmUserPasswordType.LOCAL,
                        passwd );
        role = ScmFactory.Role.createRole( sessionA, rolename, null );
        grantPriAndAttachRole( sessionA, rs, user, role,
                ScmPrivilegeType.DELETE );
        ScmAuthUtils.checkPriority( site, username, passwd, role, wsp );
    }

    private class GrantRs extends TestThreadBase {
        private ScmResource rs;
        private ScmRole role;

        public GrantRs( ScmResource rs, ScmRole role ) {
            super();
            this.rs = rs;
            this.role = role;
        }

        @Override
        public void exec() throws Exception {
            try {
                grantPriAndAttachRole( sessionA, this.rs, user, this.role,
                        ScmPrivilegeType.READ );
                grantPriAndAttachRole( sessionA, this.rs, user, this.role,
                        ScmPrivilegeType.CREATE );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private class RevokeRs extends TestThreadBase {
        private ScmResource rs;
        private ScmRole role;

        public RevokeRs( ScmResource rs, ScmRole role ) {
            super();
            this.rs = rs;
            this.role = role;
        }

        @Override
        public void exec() throws InterruptedException {
            try {
                ScmFactory.Role.revokePrivilege( sessionA, this.role, rs,
                        ScmPrivilegeType.DELETE );
                Thread.sleep( 10000 );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }
}
