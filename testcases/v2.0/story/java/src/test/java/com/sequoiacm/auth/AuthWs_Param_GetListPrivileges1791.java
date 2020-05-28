package com.sequoiacm.auth;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmPrivilege;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.element.privilege.ScmPrivilegeDefine;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-1788 :: ListPrivileges1791参数校验
 * @author fanyu
 * @Date:2018年6月11日
 * @version:1.0
 */
public class AuthWs_Param_GetListPrivileges1791 extends TestScmBase {
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private String username = "AuhtWs_1791";
    private String rolename = "1791";
    private String passwd = "1791";
    private ScmUser user;
    private ScmRole role;
    private ScmResource rs;
    private String priId;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws InterruptedException, IOException {
        try {
            site = ScmInfo.getBranchSite();
            wsp = ScmInfo.getWs();
            sessionA = TestScmTools.createSession( site );
            cleanEnv();
            prepare();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testNormalByRole() {
        getPriId();
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testGetPriById() {
        getPriId();
        // test GetPriById
        ScmPrivilege pri = null;
        try {
            pri = ScmFactory.Privilege.getPrivilegeById( sessionA, priId );
            Assert.assertEquals( pri.getPrivilege(),
                    ScmPrivilegeType.READ + "|" + ScmPrivilegeType.CREATE );
            Assert.assertEquals( pri.getRoleId(), role.getRoleId() );
            Assert.assertEquals( pri.getResource().toStringFormat(),
                    rs.toStringFormat() );
            Assert.assertEquals( pri.getRole().getRoleName(),
                    role.getRoleName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testNormalByResource() {
        ScmCursor< ScmPrivilege > cursor = null;
        try {
            cursor = ScmFactory.Privilege.listPrivilegesByResource( sessionA,
                    rs );
            int i = 0;
            while ( cursor.hasNext() ) {
                ScmPrivilege pri = cursor.getNext();
                Assert.assertEquals( pri.getResource().toStringFormat(),
                        rs.toStringFormat() );
                Assert.assertEquals( pri.getRoleType(), "role" );
                i++;
            }
            Assert.assertEquals( i >= 1, true );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testSSIsNullByRs() throws ScmException {
        ScmSession session = null;
        try {
            ScmFactory.Privilege.listPrivilegesByResource( null, rs );
            Assert.fail( "exp fail but act auccess" );
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
    private void testSSIsNullByRole() throws ScmException {
        ScmSession session = null;
        try {
            ScmFactory.Privilege.listPrivileges( session, role );
            Assert.fail( "exp fail but act auccess" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testRoleIsNull() throws ScmException {
        try {
            ScmFactory.Privilege.listPrivileges( sessionA, null );
            Assert.fail( "exp fail but act auccess" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testRsIsNull() throws ScmException {
        try {
            ScmFactory.Privilege.listPrivilegesByResource( sessionA, null );
            Assert.fail( "exp fail but act auccess" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
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
            ScmFactory.Role.revokePrivilege( sessionA, role, rs,
                    ScmPrivilegeType.CREATE );
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
            ScmUser user, ScmRole role, ScmPrivilegeType privileges )
            throws ScmException {
        ScmUserModifier modifier = new ScmUserModifier();
        ScmFactory.Role.grantPrivilege( sessionA, role, rs, privileges );
        modifier.addRole( role );
        ScmFactory.User.alterUser( sessionA, user, modifier );
    }

    private void getPriId() {
        // test listPrivileges
        ScmCursor< ScmPrivilege > cursor = null;
        try {
            cursor = ScmFactory.Privilege.listPrivileges( sessionA, role );
            int i = 0;
            while ( cursor.hasNext() ) {
                ScmPrivilege pri = cursor.getNext();
                Assert.assertEquals( pri.getRoleId(), role.getRoleId() );
                Assert.assertEquals( pri.getResource().toStringFormat(),
                        rs.toStringFormat() );
                Assert.assertEquals( pri.getPrivilege(), ScmPrivilegeDefine.READ
                        + "|" + ScmPrivilegeDefine.CREATE );
                Assert.assertEquals( pri.getRoleType(), "role" );
                priId = pri.getId();
                i++;
            }
            Assert.assertEquals( i, 1 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
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
    }

    private void prepare() throws InterruptedException {
        try {
            user = ScmFactory.User.createUser( sessionA, username,
                    ScmUserPasswordType.LOCAL, passwd );
            role = ScmFactory.Role.createRole( sessionA, rolename, null );
            rs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
            grantPriAndAttachRole( sessionA, rs, user, role,
                    ScmPrivilegeType.READ );
            grantPriAndAttachRole( sessionA, rs, user, role,
                    ScmPrivilegeType.CREATE );
            Thread.sleep( 10000 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
