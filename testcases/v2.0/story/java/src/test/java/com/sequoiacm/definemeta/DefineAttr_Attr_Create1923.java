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

/**
 * @author fanyu
 * @Description: SCM-1923 :: 普通用户创建属性
 * @Date:2018年7月5日
 * @version:1.0
 */
public class DefineAttr_Attr_Create1923 extends TestScmBase {
    private String attrname = "Create1923";
    private String desc = "Create1923";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmSession sessionNA = null;
    private ScmUser user;
    private ScmRole role;
    private String username = "Create1923";
    private String rolename = "Create1923";
    private String passwd = "1923";
    private ScmResource wsrs;
    private ScmResource dirrs;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
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
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    sessionNA );
            craeteAttr( ws );
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
                ScmPrivilegeType.READ );
        ScmFactory.Role.revokePrivilege( session, role, wsrs,
                ScmPrivilegeType.CREATE );
        ScmFactory.Role.revokePrivilege( session, role, wsrs,
                ScmPrivilegeType.DELETE );
        ScmFactory.Role.revokePrivilege( session, role, dirrs,
                ScmPrivilegeType.ALL );
        ScmFactory.Role.deleteRole( session, role );
        ScmFactory.User.deleteUser( session, user );
        if ( session != null ) {
            session.close();
        }
    }

    private void craeteAttr( ScmWorkspace ws ) throws ScmException {
        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName( attrname );
        conf.setDescription( desc );
        conf.setDisplayName( attrname + "_display" );
        conf.setRequired( true );
        conf.setType( AttributeType.INTEGER );

        ScmIntegerRule rule = new ScmIntegerRule();
        rule.setMinimum( 0 );
        rule.setMaximum( 10 );
        conf.setCheckRule( rule );
        ScmFactory.Attribute.createInstance( ws, conf );
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
        sessionNA = TestScmTools.createSession( site, username, passwd );
    }
}
