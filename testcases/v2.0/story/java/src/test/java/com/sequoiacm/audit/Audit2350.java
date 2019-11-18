package com.sequoiacm.audit;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
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
 * @Description:  SCM-2350 :: 所有的配置项都配置，服务为contentserver
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2350 extends TestScmBase {
    private String username1 = "token2350A";
    private String username2 = "token2350B";
    private String username3 = "local2350A";
    private String fileName = "file2350";
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
        ConfUtil.deleteUserAndRole(username2, username2);
        ConfUtil.deleteUserAndRole(username3, username3);
        ConfUtil.createUser(wsp, username1, ScmUserPasswordType.TOKEN, new ScmPrivilegeType[]{ScmPrivilegeType.ALL});
        ConfUtil.createUser(wsp, username2, ScmUserPasswordType.TOKEN, new ScmPrivilegeType[]{ScmPrivilegeType.ALL});
        ConfUtil.createUser(wsp, username3, ScmUserPasswordType.LOCAL, new ScmPrivilegeType[]{ScmPrivilegeType.ALL});
        session = TestScmTools.createSession(site);
        ws = ScmFactory.Workspace.getWorkspace(wsp.getName(),session);
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void test() throws ScmException, InterruptedException {
        Map<String, String> confMap = new HashMap<String, String>();
        confMap.put(ConfigCommonDefind.scm_audit_user+username1, "FILE_DML");
        confMap.put(ConfigCommonDefind.scm_audit_userType+ScmUserPasswordType.TOKEN.name(), "FILE_DQL");
        confMap.put(ConfigCommonDefind.scm_audit_userMask, "TOKEN");
        confMap.put(ConfigCommonDefind.scm_audit_mask, "FILE_DQL");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap);

        checkAudit(username1,username1,true, false);
        checkAudit(username2,username2,false, true);
        checkAudit(username3,username3,false, false);
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteUserAndRole(username1,username1);
        ConfUtil.deleteUserAndRole(username2,username2);
        ConfUtil.deleteUserAndRole(username3,username3);
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
        if (session != null) {
            session.close();
        }
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
}
