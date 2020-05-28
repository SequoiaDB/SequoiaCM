package com.sequoiacm.auth.serial;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
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
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description:SCM-2280:有前缀相同的工作区，删除前缀相同其中一个工作区，列取工作区资源
 * @author fanyu
 * @Date:2018年11月01日
 * @version:1.0
 */
public class AuthWs_ListResource2280 extends TestScmBase {
    private SiteWrapper site;
    private String wsName1 = "ws_2280_A";
    private String wsName2 = "ws_2280";
    private ScmSession sessionA;
    private ScmWorkspace wsA;
    private ScmWorkspace wsB;
    private String username = "AuhtWs_2280";
    private String rolename = "2280";
    private String passwd = "2280";
    private String dirpath = "/AuthWs_Param_GetListResource2280";
    private ScmUser user;
    private ScmRole role;
    private ScmResource dirrs;
    private ScmResource dirrs1;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            site = ScmInfo.getBranchSite();
            sessionA = TestScmTools.createSession( site );
            ScmWorkspaceUtil.deleteWs( wsName1, sessionA );
            ScmWorkspaceUtil.deleteWs( wsName2, sessionA );
            ScmWorkspaceUtil.createWS( sessionA, wsName1,
                    ScmInfo.getSiteNum() );
            ScmWorkspaceUtil.wsSetPriority( sessionA, wsName1 );

            ScmWorkspaceUtil.createWS( sessionA, wsName2,
                    ScmInfo.getSiteNum() );
            ScmWorkspaceUtil.wsSetPriority( sessionA, wsName2 );

            wsA = ScmFactory.Workspace.getWorkspace( wsName1, sessionA );
            wsB = ScmFactory.Workspace.getWorkspace( wsName2, sessionA );
            cleanEnv();
            prepare();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        testListRsByWs();
        testDelSamePrefixWs();
    }

    private void testListRsByWs() {
        // test lisResource
        ScmCursor< ScmResource > cursor = null;
        try {
            int i = 0;
            cursor = ScmFactory.Resource.listResourceByWorkspace( sessionA,
                    wsName2 );
            while ( cursor.hasNext() ) {
                ScmResource resource = cursor.getNext();
                System.out.println( "resource = " + resource.toStringFormat() );
                if ( !( resource.toStringFormat().equals( wsName2 )
                        || resource.toStringFormat().equals( wsName2
                                + ":/AuthWs_Param_GetListResource2280" ) ) ) {
                    Assert.fail( "exp true but act false" );
                }
                i++;
            }
            Assert.assertEquals( i == 2, true );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    // bug:SEQUOIACM-397
    private void testDelSamePrefixWs() throws Exception {
        ScmCursor< ScmResource > cursor = null;
        try {
            // delete wsName2 = "ws_2280"
            ScmWorkspaceUtil.deleteWs( wsName2, sessionA );
            int i = 0;
            // test list wsName1 = "ws_2280_A" resource
            cursor = ScmFactory.Resource.listResourceByWorkspace( sessionA,
                    wsName1 );
            while ( cursor.hasNext() ) {
                ScmResource resource = cursor.getNext();
                System.out.println( "resource = " + resource.toStringFormat() );
                if ( !( resource.toStringFormat().equals( wsName1 )
                        || resource.toStringFormat().equals( wsName1
                                + ":/AuthWs_Param_GetListResource2280" ) ) ) {
                    Assert.fail( "exp true but act false" );
                }
                i++;
            }
            Assert.assertEquals( i == 2, true );

            // create file for check
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName1,
                    sessionA );
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "testDelSamePrefixWs" );
            file.save();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmWorkspaceUtil.deleteWs( wsName1, sessionA );
            ScmWorkspaceUtil.deleteWs( wsName2, sessionA );
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
        ScmFactory.Role.grantPrivilege( session, role, rs, privileges );
        modifier.addRole( role );
        ScmFactory.User.alterUser( session, user, modifier );
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

    private void prepare() {
        try {
            user = ScmFactory.User.createUser( sessionA, username,
                    ScmUserPasswordType.LOCAL, passwd );
            role = ScmFactory.Role.createRole( sessionA, rolename, null );

            ScmFactory.Directory.createInstance( wsA, dirpath );
            ScmFactory.Directory.createInstance( wsB, dirpath );

            dirrs = ScmResourceFactory.createDirectoryResource( wsName1,
                    dirpath );
            dirrs1 = ScmResourceFactory.createDirectoryResource( wsName2,
                    dirpath );
            grantPriAndAttachRole( sessionA, dirrs, user, role,
                    ScmPrivilegeType.READ );
            grantPriAndAttachRole( sessionA, dirrs, user, role,
                    ScmPrivilegeType.CREATE );
            grantPriAndAttachRole( sessionA, dirrs1, user, role,
                    ScmPrivilegeType.READ );
            grantPriAndAttachRole( sessionA, dirrs1, user, role,
                    ScmPrivilegeType.CREATE );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}
