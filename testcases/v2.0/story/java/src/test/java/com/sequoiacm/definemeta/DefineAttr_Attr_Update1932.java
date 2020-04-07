package com.sequoiacm.definemeta;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
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
 * @Description:SCM-1932 :: 无权限普通用户更新属性
 * @Date:2018年7月5日
 * @version:1.0
 */
public class DefineAttr_Attr_Update1932 extends TestScmBase {
    private boolean runSuccess = false;
    private String attrname = "Update1932";
    private String desc = "Update1932";
    private ScmAttribute attr;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmSession sessionu = null;
    private ScmWorkspace ws1 = null;

    private String username = "Update1932";
    private String rolename = "Update1932";
    private ScmUser user;
    private ScmRole role;
    private String passwd = "1932";
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
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        craeteAttr();
        ScmAttribute updateAttr = ScmFactory.Attribute
                .getInstance( ws, attr.getId() );
        updateAttr.setDescription( desc + "_1" );
        updateAttr.setDisplayName( attrname + "_display_2" );
        updateAttr.setRequired( false );

        ScmAttribute updateAttr1;
        try {
            updateAttr1 = ScmFactory.Attribute.getInstance( ws1, attr.getId() );
            ScmIntegerRule rule = new ScmIntegerRule();
            rule.setMinimum( 10 );
            rule.setMaximum( 100 );

            updateAttr1.setCheckRule( rule );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        ScmAttribute actattr = ScmFactory.Attribute
                .getInstance( ws, attr.getId() );
        check( actattr, attr );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.revokePrivilege( session, role, wsrs,
                    ScmPrivilegeType.READ );
            ScmFactory.Role.deleteRole( session, role );
            ScmFactory.User.deleteUser( session, user );
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
            }
            if ( !runSuccess && attr != null ) {
                System.out.println( "class = " + attr.toString() );
                ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void craeteAttr() {
        ScmAttributeConf conf = new ScmAttributeConf();
        try {
            conf.setName( attrname );
            conf.setDescription( desc );
            conf.setDisplayName( attrname + "_display" );
            conf.setRequired( true );
            conf.setType( AttributeType.INTEGER );

            ScmIntegerRule rule = new ScmIntegerRule();
            rule.setMinimum( 0 );
            rule.setMaximum( 10 );
            conf.setCheckRule( rule );

            attr = ScmFactory.Attribute.createInstance( ws, conf );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void check( ScmAttribute actAttr, ScmAttribute expAttr ) {
        Assert.assertEquals( actAttr.getCreateUser(), expAttr.getCreateUser() );
        Assert.assertEquals( actAttr.getDescription(), desc + "_1" );
        Assert.assertEquals( actAttr.getDisplayName(),
                attrname + "_display_2" );
        Assert.assertEquals( actAttr.getName(), expAttr.getName() );
        Assert.assertEquals( actAttr.getUpdateUser(), expAttr.getUpdateUser() );
        Assert.assertEquals( actAttr.getCheckRule().toStringFormat(),
                expAttr.getCheckRule().toStringFormat() );
        Assert.assertEquals( actAttr.getCreateTime(), expAttr.getCreateTime() );
        Assert.assertEquals( actAttr.getId(), expAttr.getId() );
        Assert.assertEquals( actAttr.getType(), expAttr.getType() );
        Assert.assertEquals(
                actAttr.getUpdateTime().compareTo( expAttr.getUpdateTime() ),
                1 );
        Assert.assertEquals( actAttr.getWorkspace().getName(),
                expAttr.getWorkspace().getName() );
        Assert.assertEquals( actAttr.isRequired(), false );
        Assert.assertEquals( actAttr.isExist(), true );
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
                ScmPrivilegeType.READ );
        ScmAuthUtils.checkPriority( site, username, passwd, role, wsp );
        sessionu = TestScmTools.createSession( site, username, passwd );
        ws1 = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionu );
    }
}
