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
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-1790:getResourceById/listResourceByWorkspace参数校验
 * @author fanyu
 * @Date:2018年6月14日
 * @version:1.0
 */
public class AuthWs_Param_GetListResource1790 extends TestScmBase {
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession sessionA;
    private ScmWorkspace wsA;
    private String username = "AuhtWs_1790";
    private String rolename = "1790";
    private String passwd = "1790";
    private String dirpath = "/AuthWs_Param_GetListResource1790";
    private ScmUser user;
    private ScmRole role;
    private ScmResource wsrs;
    private ScmResource dirrs;
    private String rsId;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws InterruptedException, IOException {
        try {
            site = ScmInfo.getBranchSite();
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

    @Test(groups = { "twoSite", "fourSite" })
    private void testListRsByWs() {
        // test lisResource
        ScmCursor< ScmResource > cursor = null;
        try {
            int i = 0;
            cursor = ScmFactory.Resource
                    .listResourceByWorkspace( sessionA, wsp.getName() );
            while ( cursor.hasNext() ) {
                cursor.getNext();
                i++;
            }
            Assert.assertEquals( i >= 2, true );
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
    private void testGetRsById() {
        ScmCursor< ScmPrivilege > cursor = null;
        try {
            cursor = ScmFactory.Privilege
                    .listPrivilegesByResource( sessionA, dirrs );
            int i = 0;
            while ( cursor.hasNext() ) {
                ScmPrivilege pri = cursor.getNext();
                rsId = pri.getResourceId();
                i++;
            }
            Assert.assertEquals( i, 1 );

            ScmResource rs = ScmFactory.Resource
                    .getResourceById( sessionA, rsId );
            Assert.assertNotNull( rs.getType() );
            Assert.assertEquals( rs.toStringFormat(),
                    wsp.getName() + ":/AuthWs_Param_GetListResource1790" );
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
            ScmFactory.Resource.getResourceById( session, rsId );
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
    private void testListRsByWsNull() throws ScmException {
        try {
            ScmFactory.Resource.listResourceByWorkspace( sessionA, null );
            Assert.fail( "exp fail but act auccess" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testListRsBySSNull() throws ScmException {
        try {
            ScmFactory.Resource.listResourceByWorkspace( null, wsp.getName() );
            Assert.fail( "exp fail but act auccess" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testRsIdIsNull() throws ScmException {
        try {
            ScmFactory.Resource.getResourceById( sessionA, null );
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
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.READ );
            ScmFactory.Role.revokePrivilege( sessionA, role, wsrs,
                    ScmPrivilegeType.CREATE );
            ScmFactory.Role.revokePrivilege( sessionA, role, dirrs,
                    ScmPrivilegeType.READ );
            ScmFactory.Role.revokePrivilege( sessionA, role, dirrs,
                    ScmPrivilegeType.CREATE );
            ScmFactory.Role.deleteRole( sessionA, role );
            ScmFactory.User.deleteUser( sessionA, user );
            ScmFactory.Directory.deleteInstance( wsA, dirpath );
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
        ScmFactory.Role.grantPrivilege( sessionA, role, rs, privileges );
        modifier.addRole( role );
        ScmFactory.User.alterUser( sessionA, user, modifier );
    }

    private void cleanEnv() {
        try {
            ScmFactory.Directory.deleteInstance( wsA, dirpath );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DIR_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

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
            ScmFactory.Directory.createInstance( wsA, dirpath );
            user = ScmFactory.User
                    .createUser( sessionA, username, ScmUserPasswordType.LOCAL,
                            passwd );
            role = ScmFactory.Role.createRole( sessionA, rolename, null );
            wsrs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
            dirrs = ScmResourceFactory
                    .createDirectoryResource( wsp.getName(), dirpath );
            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.READ );
            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.CREATE );
            grantPriAndAttachRole( sessionA, dirrs, user, role,
                    ScmPrivilegeType.READ );
            grantPriAndAttachRole( sessionA, dirrs, user, role,
                    ScmPrivilegeType.CREATE );
            Thread.sleep( 10000 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
