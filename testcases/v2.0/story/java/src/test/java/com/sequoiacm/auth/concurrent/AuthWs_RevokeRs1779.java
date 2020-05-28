package com.sequoiacm.auth.concurrent;

import java.util.ArrayList;
import java.util.List;

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
 * @Description:SCM-1779 :: 对不同角色取消同一资源的权限并发
 * @Date:2018年6月15日
 * @version:1.0
 */
public class AuthWs_RevokeRs1779 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private ScmWorkspace wsA;
    private String username = "AuthWs_RevokeRs1779";
    private String[] rolenames = { "1779_A", "1779_B" };
    private List< ScmRole > roleList = new ArrayList< ScmRole >();
    private String passwd = "1546";
    private ScmUser user;
    private ScmResource rs;
    private String dirpath = "/1779_A";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
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
    private void test() {
        RevokeRs rThread1 = new RevokeRs( rs, roleList.get( 0 ) );
        RevokeRs rThread2 = new RevokeRs( rs, roleList.get( 1 ) );

        rThread1.start();
        rThread2.start();

        boolean r1Flag = rThread1.isSuccess();
        boolean r2Flag = rThread2.isSuccess();

        Assert.assertEquals( r1Flag, true, rThread1.getErrorMsg() );
        Assert.assertEquals( r2Flag, true, rThread2.getErrorMsg() );
        // check
        check( dirpath );
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

    private void check( String dirpath ) {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site, username, passwd );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            ScmFactory.Directory.getInstance( ws, dirpath );
            Assert.fail( "the user does not have priority to read" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
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
        ScmFactory.Directory.createInstance( wsA, dirpath );
        rs = ScmResourceFactory.createDirectoryResource( wsp.getName(),
                dirpath );

        user = ScmFactory.User.createUser( sessionA, username,
                ScmUserPasswordType.LOCAL, passwd );
        for ( String rolename : rolenames ) {
            ScmRole role = ScmFactory.Role.createRole( sessionA, rolename,
                    null );
            roleList.add( role );
            grantPriAndAttachRole( sessionA, rs, user, role,
                    ScmPrivilegeType.READ );
        }
        ScmAuthUtils.checkPriority( site, username, passwd,
                roleList.get( roleList.size() - 1 ), wsp );
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
                        ScmPrivilegeType.READ );
                Thread.sleep( 20000 );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }
}
