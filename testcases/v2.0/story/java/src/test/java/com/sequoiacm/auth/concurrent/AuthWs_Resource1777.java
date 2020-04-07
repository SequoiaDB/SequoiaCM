package com.sequoiacm.auth.concurrent;

import java.util.ArrayList;
import java.util.List;

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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @author fanyu
 * @Description:SCM-1777 :: 对不同角色授权资源并发
 * @Date:2018年6月15日
 * @version:1.0
 */
public class AuthWs_Resource1777 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private ScmWorkspace wsA;
    private String username = "AuthWs_Resource1777";
    private String[] rolenames = { "1777_A", "1777_B" };
    private List< ScmRole > roleList = new ArrayList< ScmRole >();
    private String passwd = "1546";
    private ScmUser user;
    private List< ScmResource > rsList = new ArrayList< ScmResource >();
    private String[] dirpaths = { "/1777_A", "/1777_B" };

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
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
        GrantRs gThread1 = new GrantRs( rsList.get( 0 ), roleList.get( 0 ) );
        GrantRs gThread2 = new GrantRs( rsList.get( 0 ), roleList.get( 1 ) );
        GrantRs gThread3 = new GrantRs( rsList.get( 1 ), roleList.get( 0 ) );

        gThread1.start();
        gThread2.start();
        gThread3.start();

        boolean g1Flag = gThread1.isSuccess();
        boolean g2Flag = gThread2.isSuccess();
        boolean g3Flag = gThread3.isSuccess();

        Assert.assertEquals( g1Flag, true, gThread1.getErrorMsg() );
        Assert.assertEquals( g2Flag, true, gThread2.getErrorMsg() );
        Assert.assertEquals( g3Flag, true, gThread3.getErrorMsg() );

        grantPriAndAttachRole( sessionA, rsList.get( 0 ), user,
                roleList.get( 0 ), ScmPrivilegeType.READ );
        grantPriAndAttachRole( sessionA, rsList.get( 0 ), user,
                roleList.get( 1 ), ScmPrivilegeType.READ );
        grantPriAndAttachRole( sessionA, rsList.get( 1 ), user,
                roleList.get( 1 ), ScmPrivilegeType.READ );

        ScmAuthUtils.checkPriority( site, username, passwd, roleList.get( 0 ),
                wsp );
        ScmAuthUtils.checkPriority( site, username, passwd, roleList.get( 1 ),
                wsp );

        // check
        for ( String path : dirpaths ) {
            check( path );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( String rolename : rolenames ) {
                    ScmFactory.Role.deleteRole( sessionA, rolename );
                }
                ScmFactory.User.deleteUser( sessionA, user );
                for ( String path : dirpaths ) {
                    ScmFactory.Directory.deleteInstance( wsA, path );
                }
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

    private void check( String dirpath ) {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site, username, passwd );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), session );
            ScmDirectory dir = ScmFactory.Directory.getInstance( ws, dirpath );
            Assert.assertEquals( dir.getPath(), dirpath + "/" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void cleanEnv() {
        for ( String rolename : rolenames ) {
            try {
                ScmFactory.Role.deleteRole( sessionA, rolename );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
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
    }

    private void prepare() throws ScmException {
        for ( String path : dirpaths ) {
            ScmFactory.Directory.createInstance( wsA, path );
            ScmResource rs = ScmResourceFactory
                    .createDirectoryResource( wsp.getName(), path );
            rsList.add( rs );
        }

        user = ScmFactory.User
                .createUser( sessionA, username, ScmUserPasswordType.LOCAL,
                        passwd );
        for ( String rolename : rolenames ) {
            ScmRole role = ScmFactory.Role
                    .createRole( sessionA, rolename, null );
            roleList.add( role );
        }
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
        public void exec() throws InterruptedException {
            ScmSession sessionA = null;
            try {
                sessionA = TestScmTools.createSession( site );
                grantPriAndAttachRole( sessionA, this.rs, user, this.role,
                        ScmPrivilegeType.READ );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            } finally {
                if ( sessionA != null ) {
                    sessionA.close();
                }
            }
        }
    }
}
