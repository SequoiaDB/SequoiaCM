package com.sequoiacm.audit;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: SCM-2351 ::配置参数校验:只测试scm.audit.user.username中的username的值带符号（.），带有空格，其他手工测试
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2351 extends TestScmBase {
    private String username1 = "token2351.A";
    private String fileName = "file2351";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
        ConfUtil.deleteUserAndRole(username1, username1);
        createUser(wsp, username1, ScmUserPasswordType.TOKEN, new ScmPrivilegeType[]{ScmPrivilegeType.ALL});
        session = TestScmTools.createSession(site);
        ws = ScmFactory.Workspace.getWorkspace(wsp.getName(),session);
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void test() throws ScmException, InterruptedException {
        test1();
        test2();
        test3();
        test4();
        test5();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteUserAndRole(username1,username1);
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
        if (session != null) {
            session.close();
        }
    }

   //scm.audit.user.username中的username的值带符号（.）
    private void test1() throws ScmException, InterruptedException {
        Map<String, String> confMap = new HashMap<String, String>();
        confMap.put(ConfigCommonDefind.scm_audit_user+username1, "FILE_DML|FILE_DQL");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap);
        checkAudit(username1,username1,true, true);
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
    }

    //scm.audit.userType.{type}中的type值缺
    private void test2() throws ScmException, InterruptedException {
        Map<String, String> confMap = new HashMap<String, String>();
        confMap.put(ConfigCommonDefind.scm_audit_userType, "FILE_DML|FILE_DQL");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap);
        checkAudit(username1,username1,false, false);
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
    }

    //value为非审计类型的值
    private void test3() throws ScmException, InterruptedException {
        Map<String, String> confMap = new HashMap<String, String>();
        confMap.put(ConfigCommonDefind.scm_audit_userType+"TOKEN", "FILE_DML$");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap);
        checkAudit(username1,username1,false, false);
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
    }

    //scm.audit.userType.{type}中的type值不对
    private void test4() throws ScmException, InterruptedException {
        Map<String, String> confMap = new HashMap<String, String>();
        confMap.put(ConfigCommonDefind.scm_audit_userType+"TOKEN1", "FILE_DML");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap);
        checkAudit(username1,username1,false, false);
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
    }

    //配置项有前后有空格
    private void test5() throws ScmException, InterruptedException {
        Map<String, String> confMap = new HashMap<String, String>();
        confMap.put(ConfigCommonDefind.scm_audit_userType+"TOKEN", " FILE_DML ");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap);
        checkAudit(username1,username1,true, false);
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
    }

    private void checkAudit(String username, String password, boolean isLogged1, boolean isLogged2) throws ScmException, InterruptedException {
        ScmId fileId = null;
        try {
            fileId = createAndQueryFile(username, password);
            BSONObject bson1 = new BasicBSONObject().append(ScmAttributeName.Audit.TYPE, "CREATE_FILE")
                    .append(ScmAttributeName.Audit.USERNAME, username);
            BSONObject bson2 = new BasicBSONObject().append(ScmAttributeName.Audit.TYPE, "FILE_DQL")
                    .append(ScmAttributeName.Audit.USERNAME, username);
            Assert.assertEquals(ConfUtil.checkAudit(session, bson1, fileId.get()), isLogged1, "Has the configuration been updated? fileId = " + fileId.get());
            Assert.assertEquals(ConfUtil.checkAudit(session, bson2, fileId.get()), isLogged2, "Has the configuration been updated? fileId = " + fileId.get());
        } finally {
            if (fileId != null) {
                ScmFactory.File.deleteInstance(ws,fileId,true);
            }
        }
    }

    private ScmId createAndQueryFile(String username, String password) throws ScmException {
        ScmSession session = null;
        ScmId fileId = null;
        try {
            session = TestScmTools.createSession(site,username,password);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName(fileName);
            fileId = file.save();
            ScmFactory.File.getInstance(ws,fileId);
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return fileId;
    }

    private void createUser(WsWrapper wsp, String name, ScmUserPasswordType passwordType,
                                  ScmPrivilegeType[] privileges) throws Exception {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession(site);
            ScmUser scmUser = ScmFactory.User.createUser(session, name, passwordType, name);
            ScmRole role = ScmFactory.Role.createRole(session, name, "desc");
            ScmResource rs = ScmResourceFactory.createWorkspaceResource(wsp.getName());
            for (ScmPrivilegeType privilege : privileges) {
                ScmFactory.Role.grantPrivilege(session, role, rs, privilege);
            }
            ScmUserModifier modifier = new ScmUserModifier();
            modifier.addRole(role);
            ScmFactory.User.alterUser(session, scmUser, modifier);
            Thread.sleep(10000);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
