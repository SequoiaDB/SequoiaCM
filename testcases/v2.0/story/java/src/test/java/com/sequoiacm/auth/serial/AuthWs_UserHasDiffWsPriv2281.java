package com.sequoiacm.auth.serial;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description: SCM-1744 :: 用户拥有多个角色，用继承的权限操作业务
 * @author fanyu
 * @Date:2018年6月8日
 * @version:1.0
 */
public class AuthWs_UserHasDiffWsPriv2281 extends TestScmBase {
    private SiteWrapper site;
    private String wsName1 = "ws_2281_A";
    private String wsName2 = "ws_2281_B";
    private ScmSession sessionA;
    private String username = "AuhtWs_2281";
    private String rolename = "2281";
    private String passwd = "2281";
    private String dirpath = "/AuthWs_Param_GetListResource2281";
    private ScmUser user;
    private ScmRole role;
    private ScmResource wsrs;
    private ScmResource dirrs;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        sessionA = TestScmTools.createSession( site );
        cleanEnv();
        ScmWorkspaceUtil.createWS( sessionA, wsName1, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( sessionA, wsName1 );
        ScmWorkspaceUtil.createWS( sessionA, wsName2, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( sessionA, wsName2 );
        try {
            user = ScmFactory.User
                    .createUser( sessionA, username, ScmUserPasswordType.LOCAL,
                            passwd );
            role = ScmFactory.Role.createRole( sessionA, rolename, null );

            wsrs = ScmResourceFactory.createWorkspaceResource( wsName1 );
            grantPriAndAttachRole( sessionA, wsrs, user, role,
                    ScmPrivilegeType.ALL );
            dirrs = ScmResourceFactory.createDirectoryResource( wsName2, "/" );
            grantPriAndAttachRole( sessionA, dirrs, user, role,
                    ScmPrivilegeType.CREATE );
            grantPriAndAttachRole( sessionA, dirrs, user, role,
                    ScmPrivilegeType.READ );
            ScmAuthUtils.checkPriority( site, username, passwd, role, wsName1 );
            ScmAuthUtils.checkPriority( site, username, passwd, role, wsName2 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void testCreateDirInWs1() {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site, username, passwd );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsName1, session );
            ScmFactory.Directory.createInstance( ws, dirpath );
            ScmDirectory dir = ScmFactory.Directory.getInstance( ws, dirpath );
            Assert.assertEquals( dir.getPath(), dirpath + "/" );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test(groups = { "fourSite" })
    private void testCreateFileInWs2() {
        ScmSession session = null;
        String fileName = "2281";
        try {
            session = TestScmTools.createSession( site, username, passwd );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsName1, session );

            // create file
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName );
            ScmId fileId = file.save();

            // get file
            ScmFile file1 = ScmFactory.File.getInstance( ws, fileId );
            Assert.assertEquals( file1.getFileName(), fileName );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
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
            ScmUser user, ScmRole role,
            ScmPrivilegeType privileges ) throws ScmException {
        ScmUserModifier modifier = new ScmUserModifier();
        ScmFactory.Role.grantPrivilege( sessionA, role, rs, privileges );
        modifier.addRole( role );
        ScmFactory.User.alterUser( sessionA, user, modifier );
    }

    private void cleanEnv() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName1, sessionA );
        ScmWorkspaceUtil.deleteWs( wsName2, sessionA );
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
}
