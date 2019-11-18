package com.sequoiacm.config;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fanyu
 * @Description: SCM-2321 :: 正常删除一组配置项
 * @Date:2018年12月13日
 * @version:1.0
 */
public class DeleteConf2321 extends TestScmBase {
    private String fileName = "file2321";
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        site = ScmInfo.getSite();
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
    }

    @Test(groups = {"twoSite", "fourSite"})
    private void test() throws Exception {
        ScmSession session = null;
        try {
            List<String> properties = new ArrayList<String>();
            properties.add(ConfigCommonDefind.scm_audit_mask);
            properties.add(ConfigCommonDefind.scm_audit_userMask);
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .service(site.getSiteServiceName())
                    .deleteProperties(properties)
                    .build();
            session = TestScmTools.createSession(site);
            ScmUpdateConfResultSet actResult = ScmSystem.Configuration.setConfigProperties(session, confProp);
            List<String> okServices = new ArrayList<String>();
            okServices.add(site.getSiteServiceName());
            ConfUtil.checkResultSet(actResult, site.getNodeNum(), 0, okServices, new ArrayList<String>());
        } finally {
            if (session != null) {
                session.close();
            }
        }

        List<String> list = new ArrayList<String>();
        list.add(ConfigCommonDefind.scm_audit_mask);
        list.add(ConfigCommonDefind.scm_audit_userMask);
        for (NodeWrapper node : site.getNodes(site.getNodeNum())) {
            ConfUtil.checkDeletedConf(node.getUrl(), list);
        }
        ConfUtil.checkNotTakeEffect(site, fileName);
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
    }
}
