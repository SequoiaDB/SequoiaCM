package com.sequoiacm.definemeta;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmClass;
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
 * @Description: SCM-1872 :: 无权限普通用户关联属性
 * @Date:2018年7月4日
 * @version:1.0
 */
public class DefineAttr_Class_ClassAttachAttr1872 extends TestScmBase {
    private boolean runSuccess = false;
    private String name = "ClassAttachAttr1872";
    private String desc = "ClassAttachAttr1872";
    private ScmClass class1 = null;
    private ScmAttribute attr = null;

    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmSession sessionu = null;
    private ScmWorkspace wsu = null;

    private String passwd = "1872";
    private ScmUser user = null;
    private ScmRole role = null;
    private ScmResource wsrs = null;
    private ScmResource dirrs = null;

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
        try {
            ScmClass class2 = ScmFactory.Class
                    .getInstance( wsu, class1.getId() );
            class2.attachAttr( attr.getId() );
            Assert.fail( "the user does not have priority to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        try {
            ScmClass class3 = ScmFactory.Class
                    .getInstance( ws, class1.getId() );
            Assert.assertEquals( class3.listAttrs().size(), 0 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ScmFactory.Role.revokePrivilege( session, role, wsrs,
                ScmPrivilegeType.UPDATE );
        ScmFactory.Role.revokePrivilege( session, role, wsrs,
                ScmPrivilegeType.CREATE );
        ScmFactory.Role
                .revokePrivilege( session, role, wsrs, ScmPrivilegeType.READ );
        ScmFactory.Role.revokePrivilege( session, role, wsrs,
                ScmPrivilegeType.DELETE );

        ScmFactory.Role
                .revokePrivilege( session, role, dirrs, ScmPrivilegeType.ALL );
        ScmFactory.Role.deleteRole( session, role );
        ScmFactory.User.deleteUser( session, user );
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Class.deleteInstance( ws, class1.getId() );
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

    private ScmAttribute craeteAttr( String name ) throws ScmException {
        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName( name );
        conf.setDescription( desc );
        conf.setDisplayName( name + "_display" );
        conf.setRequired( true );
        conf.setType( AttributeType.INTEGER );

        ScmIntegerRule rule = new ScmIntegerRule();
        rule.setMinimum( 0 );
        rule.setMaximum( 10 );
        conf.setCheckRule( rule );
        ScmAttribute attr = ScmFactory.Attribute.createInstance( ws, conf );
        return attr;
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
            ScmFactory.Role.deleteRole( session, name );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            ScmFactory.User.deleteUser( session, name );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private void prepare() throws Exception {
        class1 = ScmFactory.Class.createInstance( ws, name, desc );
        attr = craeteAttr( name );

        user = ScmFactory.User
                .createUser( session, name, ScmUserPasswordType.LOCAL, passwd );
        role = ScmFactory.Role.createRole( session, name, null );
        wsrs = ScmResourceFactory.createWorkspaceResource( wsp.getName() );
        dirrs = ScmResourceFactory
                .createDirectoryResource( wsp.getName(), "/" );

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
        ScmAuthUtils.checkPriority( site, name, passwd, role, wsp );

        sessionu = TestScmTools.createSession( site, name, passwd );
        wsu = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionu );
    }
}
