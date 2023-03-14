package com.sequoiacm.definemeta;

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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @author fanyu
 * @Description: SCM-1844 :: 普通用户创建模型
 * @Date:2018年7月4日
 * @version:1.0
 */
public class DefineAttr_Class_Create1844 extends TestScmBase {
    private String classname = "Create1844";
    private String desc = "Create1844";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmSession sessionNA;
    private ScmUser user;
    private ScmRole role;
    private String username = "Create1844";
    private String rolename = "Create1844";
    private String passwd = "1844";
    private ScmResource wsrs;
    private ScmResource dirrs;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( site );
            cleanEnv();
            prepare();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    sessionNA );
            ScmFactory.Class.createInstance( ws, classname, desc );
            Assert.fail( "the user does not have priority to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ScmFactory.Role.revokePrivilege( session, role, wsrs,
                ScmPrivilegeType.UPDATE );
        ScmFactory.Role.revokePrivilege( session, role, wsrs,
                ScmPrivilegeType.CREATE );
        ScmFactory.Role.revokePrivilege( session, role, wsrs,
                ScmPrivilegeType.DELETE );
        ScmFactory.Role.revokePrivilege( session, role, wsrs,
                ScmPrivilegeType.READ );
        ScmFactory.Role.revokePrivilege( session, role, dirrs,
                ScmPrivilegeType.ALL );
        ScmFactory.Role.deleteRole( session, role );
        ScmFactory.User.deleteUser( session, user );
        if ( session != null ) {
            session.close();
        }
    }

    private void grantPriAndAttachRole( ScmSession session, ScmResource rs,
            ScmUser user, ScmRole role, ScmPrivilegeType privileges ) {
        try {
            ScmUserModifier modifier = new ScmUserModifier();
            ScmFactory.Role.grantPrivilege( session, role, rs, privileges );
            modifier.addRole( role );
            ScmFactory.User.alterUser( session, user, modifier );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void cleanEnv() {
        try {
            ScmFactory.Role.deleteRole( session, rolename );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            ScmFactory.User.deleteUser( session, username );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private void prepare() throws Exception {
        user = ScmFactory.User.createUser( session, username,
                ScmUserPasswordType.LOCAL, passwd );
        role = ScmFactory.Role.createRole( session, rolename, null );
        wsrs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
        dirrs = ScmResourceFactory.createDirectoryResource( wsp.getName(),
                "/" );

        grantPriAndAttachRole( session, wsrs, user, role,
                ScmPrivilegeType.UPDATE );
        grantPriAndAttachRole( session, wsrs, user, role,
                ScmPrivilegeType.CREATE );
        grantPriAndAttachRole( session, wsrs, user, role,
                ScmPrivilegeType.DELETE );
        grantPriAndAttachRole( session, wsrs, user, role,
                ScmPrivilegeType.READ );
        grantPriAndAttachRole( session, dirrs, user, role,
                ScmPrivilegeType.ALL );

        ScmAuthUtils.checkPriority( site, username, passwd, role, wsp );

        sessionNA = ScmSessionUtils.createSession( site, username, passwd );
    }
}
