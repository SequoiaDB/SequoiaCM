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
 * @Description: SCM-2339:指定多个不重复的username，审计类型有重叠
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2339 extends TestScmBase {
    private String metaName = "2339";
    private String fileName = "2339";
    private String name1 = "token2339A";
    private String name2 = "token2339B";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
        ConfUtil.deleteUserAndRole(name1, name1);
        ConfUtil.deleteUserAndRole(name2, name2);
        session = TestScmTools.createSession(site);
        ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
        ConfUtil.createUser(wsp, name1, ScmUserPasswordType.TOKEN, new ScmPrivilegeType[]{ScmPrivilegeType.ALL});
        ConfUtil.createUser(wsp, name2, ScmUserPasswordType.TOKEN, new ScmPrivilegeType[]{ScmPrivilegeType.ALL});
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void test() throws ScmException {
        //test local
        Map<String, String> confMap = new HashMap<String, String>();
        confMap.put(ConfigCommonDefind.scm_audit_user + name1, "ALL");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap);

        //test token
        Map<String, String> confMap1 = new HashMap<String, String>();
        confMap1.put(ConfigCommonDefind.scm_audit_user + name2, " META_CLASS_DML|META_CLASS_DQL");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap1);
        //Verify that audit logs are generated as configured
        checkAudit(name1,name1,true,true,true);
        checkAudit(name2,name2,true,true,false);
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteUserAndRole(name1, name1);
        ConfUtil.deleteUserAndRole(name2, name2);
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
        if (session != null) {
            session.close();
        }
    }

    private void checkAudit(String username,String password,boolean isLogged1,boolean isLogged2,boolean isLogged3) throws ScmException {
        ScmId metaId = null;
        ScmId fileId = null;
        try {
            metaId = createAndQueryMeta(username, metaName);
            fileId = createFile(fileName, username);

            BSONObject bson1 = new BasicBSONObject().append(ScmAttributeName.Audit.TYPE, "CREATE_META_CLASS")
                    .append(ScmAttributeName.Audit.USERNAME, username);
            BSONObject bson2 = new BasicBSONObject().append(ScmAttributeName.Audit.TYPE, "META_CLASS_DQL")
                    .append(ScmAttributeName.Audit.USERNAME, username);
            BSONObject bson3 = new BasicBSONObject().append(ScmAttributeName.Audit.TYPE, "CREATE_FILE")
                    .append(ScmAttributeName.Audit.USERNAME, username);

            Assert.assertEquals(ConfUtil.checkAudit(session, bson1
                    , metaName), isLogged1, "Has the configuration been updated?");
            Assert.assertEquals(ConfUtil.checkAudit(session, bson2
                    , metaId.get()), isLogged2, "Has the configuration been updated?");
            Assert.assertEquals(ConfUtil.checkAudit(session,bson3
                    , fileId.get()), isLogged3, "Has the configuration been updated?");
        } finally {
            if (metaId != null) {
                ScmFactory.Class.deleteInstance(ws, metaId);
            }
            if (fileId != null) {
                ScmFactory.File.deleteInstance(ws, fileId, true);
            }
        }
    }

    private ScmId createAndQueryMeta(String username, String metaName) throws ScmException {
        ScmSession session = null;
        ScmId metaId = null;
        try {
            session = TestScmTools.createSession(site, username, username);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            //create meta
            ScmClass meta = ScmFactory.Class.createInstance(ws, metaName, "desc");
            metaId = meta.getId();
            //query meta
            ScmFactory.Class.getInstance(ws, metaId);
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return metaId;
    }

    private ScmId createFile(String fileName, String username) throws ScmException {
        ScmSession session = null;
        ScmId fileId = null;
        try {
            session = TestScmTools.createSession(site, username, username);
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName(fileName);
            fileId = file.save();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return fileId;
    }
}
