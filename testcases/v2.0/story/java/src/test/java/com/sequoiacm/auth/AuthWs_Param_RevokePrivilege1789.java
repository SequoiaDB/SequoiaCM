package com.sequoiacm.auth;

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
 * @Description:SCM-1788 :: grantPrivilege参数校验
 * @author fanyu
 * @Date:2018年6月11日
 * @version:1.0
 */
public class AuthWs_Param_RevokePrivilege1789 extends TestScmBase {
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private String username = "AuhtWs_1789";
    private String rolename = "1789";
    private String passwd = "1789";
    private ScmUser user;
    private ScmRole role;
    private ScmResource rs;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            site = ScmInfo.getBranchSite();
            wsp = ScmInfo.getWs();
            sessionA = ScmSessionUtils.createSession( site );
            cleanEnv();
            prepare();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testSSInexist() {
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( site );
            session.close();
            ScmFactory.Role.revokePrivilege( session, role, rs,
                    ScmPrivilegeType.READ );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SESSION_CLOSED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testSSIsNull() throws ScmException {
        ScmSession session = null;
        try {
            ScmFactory.Role.revokePrivilege( session, role, rs,
                    ScmPrivilegeType.READ );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testRoleInexist() throws ScmException {
        String rolename = "ROLE_1788_0";
        ScmRole role = null;
        try {
            role = ScmFactory.Role.createRole( sessionA, rolename, null );
            ScmFactory.Role.deleteRole( sessionA, role );
            ScmFactory.Role.revokePrivilege( sessionA, role, rs,
                    ScmPrivilegeType.READ );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.PRIVILEGE_REVOKE_FAILED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testRoleIsNull() throws ScmException {
        ScmRole role = null;
        try {
            ScmFactory.Role.revokePrivilege( sessionA, role, rs,
                    ScmPrivilegeType.READ );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testRsIsInexist() throws ScmException {
        ScmResource rs = null;
        String rsName = "ws_rs";
        try {
            rs = ScmResourceFactory.createWorkspaceResource( rsName );
            ScmFactory.Role.revokePrivilege( sessionA, role, rs,
                    ScmPrivilegeType.READ );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testRsIsNull() throws ScmException {
        try {
            ScmFactory.Role.revokePrivilege( sessionA, role, null,
                    ScmPrivilegeType.READ );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testPriNoEmue() throws ScmException {
        try {
            ScmFactory.Role.revokePrivilege( sessionA, role, rs,
                    ScmPrivilegeType.READ + "1" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.PRIVILEGE_REVOKE_FAILED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.revokePrivilege( sessionA, role, rs,
                    ScmPrivilegeType.READ );
            ScmFactory.Role.deleteRole( sessionA, role );
            ScmFactory.User.deleteUser( sessionA, user );
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
            ScmUser user, ScmRole role, ScmPrivilegeType privileges ) {
        try {
            ScmUserModifier modifier = new ScmUserModifier();
            ScmFactory.Role.grantPrivilege( sessionA, role, rs, privileges );
            modifier.addRole( role );
            ScmFactory.User.alterUser( sessionA, user, modifier );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
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
    }

    private void prepare() throws Exception {
        try {
            user = ScmFactory.User.createUser( sessionA, username,
                    ScmUserPasswordType.LOCAL, passwd );
            role = ScmFactory.Role.createRole( sessionA, rolename, null );
            rs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
            grantPriAndAttachRole( sessionA, rs, user, role,
                    ScmPrivilegeType.READ );
            ScmAuthUtils.checkPriority( site, username, passwd, role, wsp );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
