package com.sequoiacm.auth.concurrent;

import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
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
    private ScmSession newUserSession;
    private String subdirName = "1778";
    private String username = "AuthWs_GrantAndRevoke1778";
    private String rolename = "1778_R";
    private String passwd = "1778";
    private ScmUser user;
    private ScmRole role;
    private String dirpath = "/AuthWs_GrantAndRevoke1778";
    private ScmResource rs;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        sessionA = ScmSessionUtils.createSession( site );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        cleanEnv();
        prepare();
        newUserSession = ScmSessionUtils.createSession( site, username, passwd );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    private void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new GrantRs( rs, role ) );
        t.addWorker( new RevokeRs( rs, role ) );
        t.run();

        checkCreateAndRead( newUserSession, subdirName );
        checkDelete( newUserSession, subdirName );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
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
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( newUserSession != null ) {
                newUserSession.close();
            }
        }
    }

    private void checkCreateAndRead( ScmSession session, String subname )
            throws Exception {
        String subpath = dirpath + "/" + subname + "/";
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                session );
        int times = 0;
        while ( true ) {
            try {
                ScmDirectory dir = ScmFactory.Directory.getInstance( ws,
                        dirpath );
                Assert.assertEquals( dir.getPath(), dirpath + "/" );
                dir.createSubdirectory( subname );
                ScmDirectory subdir = ScmFactory.Directory.getInstance( ws,
                        subpath );
                Assert.assertEquals( subdir.getName(), subname );
                break;
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                    throw e;
                } else {
                    Thread.sleep( 1000 );
                    times++;
                    if ( times >= 30 ) {
                        throw new Exception( "check create and read time out" );
                    }
                }
            }
        }
    }

    private void grantPriAndAttachRole( ScmSession session, ScmResource rs,
            ScmUser user, ScmRole role, ScmPrivilegeType privileges )
            throws ScmException {
        ScmUserModifier modifier = new ScmUserModifier();
        ScmFactory.Role.grantPrivilege( session, role, rs, privileges );
        modifier.addRole( role );
        ScmFactory.User.alterUser( session, user, modifier );
    }

    private void checkDelete( ScmSession session, String subname )
            throws Exception {
        String subpath = dirpath + "/" + subname + "/";
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                session );
        int times = 0;
        while ( true ) {
            try {
                ScmFactory.Directory.deleteInstance( ws, subpath );
                ScmFactory.Directory.createInstance( ws, subpath );
                times++;
                if ( times >= 30 ) {
                    Thread.sleep( 1000 );
                    throw new Exception(
                            "the user does not have priority to delete subpath = "
                                    + subpath );
                }
            } catch ( ScmException e ) {
                if ( e.getError() == ScmError.OPERATION_UNAUTHORIZED ) {
                    break;
                } else {
                    throw e;
                }
            } finally {
                ScmFactory.Directory.deleteInstance( wsA, subpath );
            }
        }
    }

    private void cleanEnv() throws ScmException {
        try {
            ScmFactory.Role.deleteRole( sessionA, rolename );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
        try {
            ScmFactory.User.deleteUser( sessionA, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }

        try {
            ScmFactory.Directory.deleteInstance( wsA, dirpath );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                throw e;
            }
        }
    }

    private void prepare() throws Exception {
        rs = ScmResourceFactory.createDirectoryResource( wsp.getName(),
                dirpath );
        ScmFactory.Directory.createInstance( wsA, dirpath );

        user = ScmFactory.User.createUser( sessionA, username,
                ScmUserPasswordType.LOCAL, passwd );
        role = ScmFactory.Role.createRole( sessionA, rolename, null );
        grantPriAndAttachRole( sessionA, rs, user, role,
                ScmPrivilegeType.DELETE );
        ScmAuthUtils.checkPriority( site, username, passwd, role, wsp );
    }

    private class GrantRs {
        private ScmResource rs;
        private ScmRole role;

        public GrantRs( ScmResource rs, ScmRole role ) {
            super();
            this.rs = rs;
            this.role = role;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws Exception {
            grantPriAndAttachRole( sessionA, this.rs, user, this.role,
                    ScmPrivilegeType.READ );
            grantPriAndAttachRole( sessionA, this.rs, user, this.role,
                    ScmPrivilegeType.CREATE );
        }
    }

    private class RevokeRs {
        private ScmResource rs;
        private ScmRole role;

        public RevokeRs( ScmResource rs, ScmRole role ) {
            super();
            this.rs = rs;
            this.role = role;
        }

        @ExecuteOrder(step = 1)
        public void exec() throws InterruptedException, ScmException {
            ScmFactory.Role.revokePrivilege( sessionA, this.role, rs,
                    ScmPrivilegeType.DELETE );
        }
    }
}
