package com.sequoiacm.workspace.serial;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-6166:非管理员用户使用驱动更新工作区元数据域 
 *              SCM-6167:非管理员用户使用驱动更新工作区元数据域不存在
 * @Author yangjianbo
 * @CreateDate 2023/5/11
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class UpdateDomain6166_6167 extends TestScmBase {
    private ScmSession session = null;
    private ScmSession userSession = null;
    private SiteWrapper rootSite = null;
    private String wsName = "ws6166";
    private ScmWorkspace ws;
    private String doMainNew = wsName + "domainnew";
    private String doMainNotExit = wsName + "domainnotexit";
    private String userName = "user6166";
    private String roleName = "role6166";
    private String pwd = "admin";
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        cleanEnv();
        prepare();
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ws = ScmFactory.Workspace.getWorkspace( wsName, userSession );
        try {
            ws.updateMetaDomain( doMainNotExit );
            Assert.fail( "not admin user update doMain should be failed" );
        } catch ( ScmException ex ) {
            if ( ex.getErrorCode() != ScmError.OPERATION_UNAUTHORIZED
                    .getErrorCode() ) {
                throw ex;
            }
        }

        try {
            ws.updateMetaDomain( doMainNew );
            Assert.fail( "not admin user update doMain should be failed" );
        } catch ( ScmException ex ) {
            if ( ex.getErrorCode() != ScmError.OPERATION_UNAUTHORIZED
                    .getErrorCode() ) {
                throw ex;
            }
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                cleanEnv();
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void cleanEnv() throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        TestSdbTools.dropDomain( rootSite, doMainNew );
        TestSdbTools.dropDomain( rootSite, doMainNotExit );
        ScmAuthUtils.deleteUser( session, userName );
        try {
            ScmFactory.Role.deleteRole( session, roleName );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
    }

    private void prepare() throws ScmException, InterruptedException {
        ScmUser user = ScmAuthUtils.createUser( session, userName, pwd );
        userSession = ScmSessionUtils
                .createSession( rootSite.getSiteServiceName(), userName, pwd );

        ws = ScmWorkspaceUtil.createWS( session, wsName, rootSite.getSiteId() );
        TestSdbTools.createDomain( rootSite, doMainNew );

        ScmRole role = ScmAuthUtils.createRole( session, roleName );
        ScmResource resource = ScmResourceFactory
                .createWorkspaceResource( wsName );
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
    }

}
