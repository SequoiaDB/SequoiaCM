package com.sequoiacm.audit;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @Description:SCM-2337:指定多个重复的userType,审计类型有重叠
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2337 extends TestScmBase {
    private String fileName = "2337";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
        session = TestScmTools.createSession(site);
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void test() throws ScmException {
        Map<String, String> confMap = new HashMap<String, String>();
        confMap.put(ConfigCommonDefind.scm_audit_userType + ScmUserPasswordType.LOCAL.name(), "ALL");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap);

        Map<String, String> confMap1 = new HashMap<String, String>();
        confMap1.put(ConfigCommonDefind.scm_audit_userType + ScmUserPasswordType.LOCAL.name(), "DIR_DML");
        ConfUtil.updateConf(site.getSiteServiceName(), confMap1);
        checkAudit();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
        if (session != null) {
            session.close();
        }
    }

    private void checkAudit() throws ScmException {
        ScmId fileId = null;
        String dirPath = "/2337";
        ScmDirectory dir = null;
        ScmSession session = null;
        ScmWorkspace ws = null;
        try {
            session = TestScmTools.createSession(site);
            fileId = createFile(fileName + "_" + UUID.randomUUID());
            //check audit is logged by new configuration
            Assert.assertEquals(ConfUtil.checkAudit(session, new BasicBSONObject().append(ScmAttributeName.Audit.TYPE, "CREATE_FILE")
                    , fileId.get()), false, "Has not the configuration been updated?");
            //create directory for check
            ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
            dir = ScmFactory.Directory.createInstance(ws, dirPath);
            Assert.assertEquals(ConfUtil.checkAudit(session, new BasicBSONObject().append(ScmAttributeName.Audit.TYPE, "CREATE_DIR")
                    , dir.getId()), true, "Has the configuration been updated?");
        } finally {
            if (fileId != null) {
                ScmFactory.File.deleteInstance(ws, fileId, true);
            }
            if (dir != null) {
                ScmFactory.Directory.deleteInstance(ws, dirPath);
            }
            if (session != null) {
                session.close();
            }
        }
    }

    private ScmId createFile(String fileName) throws ScmException {
        ScmSession session = null;
        ScmId fileId = null;
        try {
            session = TestScmTools.createSession(site);
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
