package com.sequoiacm.config.concurrent;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fanyu
 * @Description: SCM-2325 :: 并发删除和更新同一配置
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateAndDeleteConf2325 extends TestScmBase {
    private String fileName = "file2325";
    private SiteWrapper site = null;
    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
        filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile(localPath);
        TestTools.LocalFile.createDir(localPath.toString());
        TestTools.LocalFile.createFile(filePath, fileSize);
        site = ScmInfo.getRootSite();
        ConfUtil.deleteAuditConf(site.getSiteServiceName());

    }

    @Test(groups = {"twoSite", "fourSite"})
    private void test() throws Exception {
        Update uThraed = new Update();
        Delete dThread = new Delete();
        uThraed.start();
        dThread.start();
        Assert.assertEquals(uThraed.isSuccess(), true, uThraed.getErrorMsg());
        Assert.assertEquals(dThread.isSuccess(), true, dThread.getErrorMsg());

        update();

        //check local configuration
        Map<String, String> map = new HashMap<String, String>();
        map.put(ConfigCommonDefind.scm_audit_mask, "ALL");
        map.put(ConfigCommonDefind.scm_audit_userMask, "LOCAL");
        for (NodeWrapper node : site.getNodes(site.getNodeNum())) {
            ConfUtil.checkUpdatedConf(node.getUrl(), map);
        }
       //check updated conf take effect
        ConfUtil.checkTakeEffect(site, fileName);
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
        TestTools.LocalFile.removeFile(localPath);
    }

    private class Update extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmUpdateConfResultSet actResults = update();
            List<String> expServiceNames = new ArrayList<String>();
            expServiceNames.add(site.getSiteServiceName());
            ConfUtil.checkResultSet(actResults, site.getNodeNum(), 0, expServiceNames, new ArrayList<String>());
        }
    }

    private class Delete extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmUpdateConfResultSet actResult = null;
            try {
                session = TestScmTools.createSession(site);
                ScmConfigProperties confProp = ScmConfigProperties.builder()
                        .service(site.getSiteServiceName())
                        .deleteProperty(ConfigCommonDefind.scm_audit_mask)
                        .deleteProperty(ConfigCommonDefind.scm_audit_userMask)
                        .build();
                actResult = ScmSystem.Configuration.setConfigProperties(session, confProp);
                List<String> expServiceNames = new ArrayList<String>();
                expServiceNames.add(site.getSiteServiceName());
                ConfUtil.checkResultSet(actResult, site.getNodeNum(), 0, expServiceNames, new ArrayList<String>());
            } finally {
                if (session != null) {
                    session.close();
                }
            }
        }
    }

    private ScmUpdateConfResultSet update() throws ScmException {
        ScmSession session = null;
        ScmUpdateConfResultSet actResult = null;
        try {
            session = TestScmTools.createSession(site);
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .service(site.getSiteServiceName())
                    .updateProperty(ConfigCommonDefind.scm_audit_mask, "ALL")
                    .updateProperty(ConfigCommonDefind.scm_audit_userMask, "LOCAL")
                    .build();
            actResult = ScmSystem.Configuration.setConfigProperties(session, confProp);
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return  actResult;
    }
}
