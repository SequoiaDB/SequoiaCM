package com.sequoiacm.definemeta;

import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @Description: SCM-1875 :: 无权限普通用户解除属性
 * @author fanyu
 * @Date:2018年7月4日
 * @version:1.0
 */
public class DefineAttr_Class_ClassDetachAttr1875 extends TestScmBase {
    private boolean runSuccess = false;
    private String name = "ClassDetachAttr1875";
    private String desc = "ClassDetachAttr1875";
    private String passwd = "1875";
    private ScmClass class1 = null;
    private ScmAttribute attr = null;

    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmSession sessionu = null;
    private ScmWorkspace wsu = null;

    private ScmUser user = null;
    private ScmRole role = null;
    private ScmResource wsrs;
    private ScmResource dirrs;

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

    @Test
    private void test() throws ScmException {
        try {
            ScmClass class2 = ScmFactory.Class.getInstance( wsu,
                    class1.getId() );
            class2.detachAttr( attr.getId() );
            Assert.fail( "the user does not have priority to do something" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNAUTHORIZED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        ScmClass class3 = ScmFactory.Class.getInstance( ws, class1.getId() );
        Assert.assertEquals( class3.listAttrs().size(), 1,
                class3.listAttrs().toString() );
        Assert.assertEquals( class3.listAttrs().get( 0 ).getId(), attr.getId(),
                class3.listAttrs().get( 0 ).toString() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.Class.deleteInstance( ws, class1.getId() );
                ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
            }
            if ( !runSuccess && attr != null ) {
                System.out.println( "class = " + class1.toString() );
                ScmFactory.Class.deleteInstance( ws, class1.getId() );
                ScmFactory.Attribute.deleteInstance( ws, attr.getId() );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
            if ( sessionu != null ) {
                sessionu.close();
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
        class1.attachAttr( attr.getId() );

        user = ScmFactory.User.createUser( session, name,
                ScmUserPasswordType.LOCAL, passwd );
        role = ScmFactory.Role.createRole( session, name, null );
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
        ScmAuthUtils.checkPriority( site, name, passwd, role, wsp );
        sessionu = TestScmTools.createSession( site, name, passwd );
        wsu = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionu );
    }
}
