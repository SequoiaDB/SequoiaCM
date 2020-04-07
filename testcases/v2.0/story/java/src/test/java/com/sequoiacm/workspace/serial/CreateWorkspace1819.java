/**
 *
 */
package com.sequoiacm.workspace.serial;

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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description CreateWorkspace1818.java 无ws管理权限用户创建ws 
 * @author luweikang
 * @date 2018年6月22日
 */
public class CreateWorkspace1819 extends TestScmBase {

    private String wsName = "ws1819";
    private ScmSession session1 = null;
    private ScmSession session2 = null;
    private SiteWrapper rootSite = null;
    private String username = "user1819";
    private String password = "passwd1819";
    private String roleName = "role1819";

    @BeforeClass
    private void setUp() throws Exception {

        rootSite = ScmInfo.getRootSite();
        session1 = TestScmTools.createSession( rootSite );
        ScmWorkspaceUtil.deleteWs( wsName, session1 );
        try {
            ScmFactory.User.deleteUser( session1, username );
            ScmFactory.Role.deleteRole( session1, roleName );
        } catch ( ScmException e ) {
        }
    }

    @Test(groups = { "one", "twoSite", "fourSite" })
    private void test() throws ScmException, InterruptedException {

        this.createScmUser();
        int siteNum = ScmInfo.getSiteNum();
        try {
            ScmWorkspaceUtil.createWS( session2, wsName, siteNum );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.OPERATION_UNAUTHORIZED,
                    e.getMessage() );
        }

    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.User.deleteUser( session1, username );
            ScmFactory.Role.deleteRole( session1, roleName );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( session1 != null ) {
                session1.close();
            }
        }
    }

    private void createScmUser() throws ScmException, InterruptedException {
        ScmUser user = ScmFactory.User
                .createUser( session1, username, ScmUserPasswordType.LOCAL,
                        password );
        ScmRole role = ScmFactory.Role.createRole( session1, roleName, null );
        ScmUserModifier modifier = new ScmUserModifier();
        ScmResource rs = ScmResourceFactory
                .createWorkspaceResource( ScmInfo.getWs().getName() );
        ScmFactory.Role
                .grantPrivilege( session1, role, rs, ScmPrivilegeType.READ );
        modifier.addRole( role );
        ScmFactory.User.alterUser( session1, user, modifier );
        session2 = TestScmTools.createSession( rootSite, username, password );
    }
}
