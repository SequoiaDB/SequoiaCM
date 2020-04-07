package com.sequoiacm.definemeta;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmClass;
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
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @author fanyu
 * @Description:SCM-1847 :: 更新模型
 * @Date:2018年7月4日
 * @version:1.0
 */
public class DefineAttr_Class_Update1847 extends TestScmBase {
    private boolean runSuccess = false;
    private String classname = "Update1847";
    private String desc = "Update1847";
    private ScmClass scmClass = null;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmSession sessionA;
    private ScmWorkspace wsA = null;
    private ScmUser user;
    private ScmRole role;
    private String username = "Update1847";
    private String rolename = "Update1847";
    private String passwd = "1847";
    private ScmResource wsrs;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            cleanEnv();
            prepare();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        ScmClass class1 = null;
        try {
            class1 = ScmFactory.Class.getInstance( ws, scmClass.getId() );
            class1.setDescription( desc + "_" + 1 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        ScmClass class2 = null;
        try {
            class2 = ScmFactory.Class.getInstance( wsA, scmClass.getId() );
            class2.setDescription( desc + "_" + 2 );
            class2.setName( classname + "_" + 2 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        check( class2, scmClass );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.revokePrivilege( session, role, wsrs,
                    ScmPrivilegeType.ALL );
            ScmFactory.Role.deleteRole( session, role );
            ScmFactory.User.deleteUser( session, user );
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Class.deleteInstance( ws, scmClass.getId() );
            }
            if ( !runSuccess && scmClass != null ) {
                System.out.println( "class = " + scmClass.toString() );
                ScmFactory.Class.deleteInstance( ws, scmClass.getId() );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void grantPriAndAttachRole( ScmSession session, ScmResource rs,
            ScmUser user, ScmRole role,
            ScmPrivilegeType privileges ) {
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

    private void check( ScmClass actClass, ScmClass expClass ) {
        Assert.assertEquals( actClass.getId(), expClass.getId() );
        Assert.assertEquals( actClass.getName(), classname + "_" + 2 );
        Assert.assertEquals( actClass.getDescription(), desc + "_" + 2 );
        Assert.assertEquals( actClass.getCreateUser(),
                expClass.getCreateUser() );
        Assert.assertEquals( actClass.getUpdateUser(), username );
        Assert.assertEquals( actClass.getWorkspace().getName(),
                expClass.getWorkspace().getName() );
        Assert.assertEquals( actClass.listAttrs(), expClass.listAttrs() );
        Assert.assertEquals( actClass.getCreateTime(),
                expClass.getCreateTime() );
        Assert.assertEquals(
                actClass.getUpdateTime().compareTo( expClass.getUpdateTime() ),
                1 );
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
        user = ScmFactory.User
                .createUser( session, username, ScmUserPasswordType.LOCAL,
                        passwd );
        role = ScmFactory.Role.createRole( session, rolename, null );
        wsrs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );

        grantPriAndAttachRole( session, wsrs, user, role,
                ScmPrivilegeType.ALL );

        ScmAuthUtils.checkPriority( site, username, passwd, role, wsp );
        sessionA = TestScmTools.createSession( site, username, passwd );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

        scmClass = ScmFactory.Class.createInstance( ws, classname, desc );
    }
}
