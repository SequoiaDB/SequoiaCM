/**
 *
 */
package com.sequoiacm.workspace;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
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
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:delete workspace ,the user dose not have permission to delete
 * testlink-case:SCM-1821
 *
 * @author wuyan
 * @Date 2018.06.21
 * @version 1.00
 */
public class DeleteWorkSpace1821 extends TestScmBase {
    private static SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws1821";
    private String newUsername = "admin1821";
    private String newPassword = "admin1821";
    private String roleName = "role1821";

    @BeforeClass
    private void setUp() throws ScmException, InterruptedException {
        site = ScmInfo.getRootSite();
        session = TestScmTools.createSession( site );
        try {
            ScmFactory.Workspace.deleteWorkspace( session, wsName );
            for ( int i = 0; i < 10; i++ ) {
                Thread.sleep( 1000 );
                try {
                    ScmFactory.Workspace.getWorkspace( wsName, session );
                } catch ( ScmException e ) {
                    break;
                }
            }
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST ) {
                throw e;
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws ScmException, InterruptedException {
        int siteNum = 2;
        ScmWorkspaceUtil.createWS( session, wsName, siteNum );
        deleteWorkspaceByOtherUser();
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
            ScmFactory.User.deleteUser( session, newUsername );
            ScmFactory.Role.deleteRole( session, roleName );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void deleteWorkspaceByOtherUser()
            throws ScmException, InterruptedException {
        for ( int i = 0; i < 10; i++ ) {
            Thread.sleep( 1000 );
            try {
                ScmFactory.Workspace.getWorkspace( wsName, session );
                break;
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST ) {
                    throw e;
                }
            }
        }
        ScmUser user = ScmFactory.User.createUser( session, newUsername,
                ScmUserPasswordType.LOCAL, newPassword );
        ScmRole role = ScmFactory.Role.createRole( session, roleName, null );
        ScmUserModifier modifier = new ScmUserModifier();
        ScmResource rs = ScmResourceFactory.createWorkspaceResource( wsName );
        ScmFactory.Role.grantPrivilege( session, role, rs,
                ScmPrivilegeType.CREATE );
        ScmFactory.Role.grantPrivilege( session, role, rs,
                ScmPrivilegeType.READ );
        modifier.addRole( role );
        ScmFactory.User.alterUser( session, user, modifier );

        ScmSession newSession = TestScmTools.createSession( site, newUsername,
                newPassword );

        // delete ws fail by no privilege user
        try {
            ScmFactory.Workspace.deleteWorkspace( newSession, wsName, true );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.OPERATION_UNAUTHORIZED,
                    e.getMessage() );
        }

        // write success of the ws
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                newSession );
        byte[] writeData = new byte[ 1024 * 200 ];
        VersionUtils.createFileByStream( ws, "file1821", writeData );
    }
}
