package com.sequoiacm.config;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fanyu
 * @Description: SCM-2294 :: 修改配置失败后，再次修改正确的配置
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateConfAfterFail2294 extends TestScmBase {
    private String fileName = "file2294";
    private SiteWrapper updatedSite = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        updatedSite = ScmInfo.getSite();
        ConfUtil.deleteAuditConf(updatedSite.getSiteServiceName());
    }

    @Test(groups = {"twoSite", "fourSite"})
    private void test() throws Exception {
        //update configuration failed
        ScmSession session = null;
        try {
            session = TestScmTools.createSession(updatedSite);
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .acceptUnknownProperties(true)
                    .service(updatedSite.getSiteServiceName())
                    .updateProperty("server.port", "15200")   //it is not allowed to be updated
                    .build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration.setConfigProperties(session, confProp);
            List<String> expServiceNames = new ArrayList<String>();
            expServiceNames.add(updatedSite.getSiteServiceName());
            ConfUtil.checkResultSet(actResults, 0, updatedSite.getNodeNum(), new ArrayList<String>(), expServiceNames);
        } finally {
            if (session != null) {
                session.close();
            }
        }

        //update configuration again
        ScmSession session1 = null;
        try {
            session1 = TestScmTools.createSession(updatedSite);
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .acceptUnknownProperties(true)
                    .service(updatedSite.getSiteServiceName())
                    .updateProperty(ConfigCommonDefind.scm_audit_mask, "FILE_DML")
                    .updateProperty(ConfigCommonDefind.scm_audit_userMask, "LOCAL").build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration.setConfigProperties(session1, confProp);
            List<String> expServiceNames = new ArrayList<String>();
            expServiceNames.add(updatedSite.getSiteServiceName());
            ConfUtil.checkResultSet(actResults, updatedSite.getNodeNum(), 0, expServiceNames, new ArrayList<String>());
        } finally {
            if (session1 != null) {
                session1.close();
            }
        }
        //check updated configuration take effect
        ConfUtil.checkTakeEffect(updatedSite, fileName);
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf(updatedSite.getSiteServiceName());
    }
}
