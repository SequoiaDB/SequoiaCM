package com.sequoiacm.config;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author fanyu
 * @Description: SCM-2331 :: 指定不拥有的admin角色的用户，删除配置
 * @Date:2018年12月04日
 * @version:1.0
 */
public class DeleteConfByNoAdmin2331 extends TestScmBase {
    private String fileName = "file2331";
    private ScmSession session = null;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private String username = "DeleteConfByNoAdmin2331";
    private String rolename = "2331";
    private String passwd = "2331";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession(site);
        try {
            ScmFactory.Role.deleteRole(session, rolename);
        } catch (ScmException e) {
            System.out.println("msg =" + e.getMessage());
        }
        try {
            ScmFactory.User.deleteUser(session, username);
        } catch (ScmException e) {
            System.out.println("msg =" + e.getMessage());
        }
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
        createUser();
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void test() throws ScmException {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession(site, username, passwd);
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .acceptUnknownProperties(true)
                    .service(site.getSiteServiceName())
                    .deleteProperty(ConfigCommonDefind.scm_audit_mask)
                    .deleteProperty(ConfigCommonDefind.scm_audit_userMask)
                    .build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration.setConfigProperties(session, confProp);
            Assert.fail("delete configuration should be failed by normal user,actResults = " + actResults.toString());
        } catch (ScmException e) {
            if (e.getError() != ScmError.HTTP_FORBIDDEN) {
                Assert.fail(e.getMessage());
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
        //check updated configuration do not take effect
        ConfUtil.checkNotTakeEffect(site, fileName);
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        if (session != null) {
            ScmFactory.Role.deleteRole(session, rolename);
            ScmFactory.User.deleteUser(session, username);
            session.close();
        }
    }

    private void createUser() throws Exception {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession(site);
            ScmUser scmUser = ScmFactory.User.createUser(session, username, ScmUserPasswordType.LOCAL, passwd);
            ScmRole role = ScmFactory.Role.createRole(session, rolename, "");
            ScmUserModifier modifier = new ScmUserModifier();
            ScmResource resource = ScmResourceFactory.createWorkspaceResource(wsp.getName());
            ScmFactory.Role.grantPrivilege(session, role, resource, ScmPrivilegeType.ALL);
            modifier.addRole(rolename);
            ScmFactory.User.alterUser(session, scmUser, modifier);
            ScmAuthUtils.checkPriority(site, username, passwd, role, wsp);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
