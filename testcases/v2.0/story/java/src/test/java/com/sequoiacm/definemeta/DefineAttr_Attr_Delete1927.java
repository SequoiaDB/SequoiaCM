
package com.sequoiacm.definemeta;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiadb.exception.BaseException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author fanyu
 * @Description: SCM-1927 :: 无权限用户删除属性
 * @Date:2018年7月5日
 * @version:1.0
 */
public class DefineAttr_Attr_Delete1927 extends TestScmBase {
    private boolean runSuccess;
    private String attrname = "Delete1927";
    private String desc = "Delete1927";
    private ScmAttribute attr;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmSession sessionNA = null;
    private ScmUser user;
    private ScmRole role;
    private String username = "Delete1927";
    private String rolename = "Delete1927";
    private String passwd = "1927";
    private ScmResource wsrs;
    private ScmResource dirrs;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession(site);
            ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            cleanEnv();
            prepare();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void test() throws ScmException {
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionNA);
            ScmFactory.Attribute.deleteInstance(ws, attr.getId());
            Assert.fail("the user does not have priority to do something");
        } catch (ScmException e) {
            if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        }

        ScmAttribute actAttr = ScmFactory.Attribute.getInstance(ws, attr.getId());
        check(actAttr, attr);
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ScmFactory.Role.revokePrivilege(session, role, wsrs, ScmPrivilegeType.UPDATE);
        ScmFactory.Role.revokePrivilege(session, role, wsrs, ScmPrivilegeType.CREATE);
        ScmFactory.Role.revokePrivilege(session, role, wsrs, ScmPrivilegeType.DELETE);
        ScmFactory.Role.revokePrivilege(session, role, wsrs, ScmPrivilegeType.READ);

        ScmFactory.Role.revokePrivilege(session, role, dirrs, ScmPrivilegeType.ALL);
        ScmFactory.Role.deleteRole(session, role);
        ScmFactory.User.deleteUser(session, user);
        try {
            if (runSuccess || TestScmBase.forceClear) {
                ScmFactory.Attribute.deleteInstance(ws, attr.getId());
            }
            if (!runSuccess && attr != null) {
                System.out.println("class = " + attr.toString());
                ScmFactory.Attribute.deleteInstance(ws, attr.getId());
            }
        } catch (BaseException | ScmException e) {
            Assert.fail(e.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private void craeteAttr(ScmWorkspace ws) throws ScmException {
        ScmAttributeConf conf = new ScmAttributeConf();
        conf.setName(attrname);
        conf.setDescription(desc);
        conf.setDisplayName(attrname + "_display");
        conf.setRequired(true);
        conf.setType(AttributeType.INTEGER);

        ScmIntegerRule rule = new ScmIntegerRule();
        rule.setMinimum(0);
        rule.setMaximum(10);
        conf.setCheckRule(rule);
        attr = ScmFactory.Attribute.createInstance(ws, conf);
    }

    private void grantPriAndAttachRole(ScmSession session, ScmResource rs, ScmUser user, ScmRole role,
                                       ScmPrivilegeType privileges) {
        try {
            ScmUserModifier modifier = new ScmUserModifier();
            ScmFactory.Role.grantPrivilege(session, role, rs, privileges);
            modifier.addRole(role);
            ScmFactory.User.alterUser(session, user, modifier);
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private void check(ScmAttribute actAttr, ScmAttribute expAttr) {
        Assert.assertEquals(actAttr.getCreateUser(), expAttr.getCreateUser());
        Assert.assertEquals(actAttr.getDescription(), expAttr.getDescription());
        Assert.assertEquals(actAttr.getDisplayName(), expAttr.getDisplayName());
        Assert.assertEquals(actAttr.getName(), expAttr.getName());
        Assert.assertEquals(actAttr.getUpdateUser(), expAttr.getUpdateUser());
        Assert.assertEquals(actAttr.getCheckRule().toStringFormat(), expAttr.getCheckRule().toStringFormat());
        Assert.assertEquals(actAttr.getCreateTime(), expAttr.getCreateTime());
        Assert.assertEquals(actAttr.getId(), expAttr.getId());
        Assert.assertEquals(actAttr.getType(), expAttr.getType());
        Assert.assertEquals(actAttr.getUpdateTime(), expAttr.getUpdateTime());
        Assert.assertEquals(actAttr.getWorkspace().getName(), expAttr.getWorkspace().getName());
        Assert.assertEquals(actAttr.isRequired(), true);
        Assert.assertEquals(actAttr.isExist(), true);
    }

    private void cleanEnv() {
        try {
            ScmFactory.Role.deleteRole(session, rolename);
        } catch (ScmException e) {
            if (e.getError() != ScmError.HTTP_NOT_FOUND) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        }
        try {
            ScmFactory.User.deleteUser(session, username);
        } catch (ScmException e) {
            if (e.getError() != ScmError.HTTP_NOT_FOUND) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        }
    }

    private void prepare() throws Exception {
        user = ScmFactory.User.createUser(session, username, ScmUserPasswordType.LOCAL, passwd);
        role = ScmFactory.Role.createRole(session, rolename, null);
        wsrs = ScmResourceFactory.createWorkspaceResource(wsp.getName());
        dirrs = ScmResourceFactory.createDirectoryResource(wsp.getName(), "/");

        grantPriAndAttachRole(session, wsrs, user, role, ScmPrivilegeType.UPDATE);
        grantPriAndAttachRole(session, wsrs, user, role, ScmPrivilegeType.CREATE);
        grantPriAndAttachRole(session, wsrs, user, role, ScmPrivilegeType.DELETE);
        grantPriAndAttachRole(session, wsrs, user, role, ScmPrivilegeType.READ);

        grantPriAndAttachRole(session, dirrs, user, role, ScmPrivilegeType.ALL);

        ScmAuthUtils.checkPriority(site, username, passwd, role, wsp);

        sessionNA = TestScmTools.createSession(site, username, passwd);
        craeteAttr(ws);
    }
}
