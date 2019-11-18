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
 * @Description: SCM-2286 :: 指定动态生效类型为单个实例，修改配置
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateInstanceConf2286 extends TestScmBase {
    private String fileName = "file2286";
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        site = ScmInfo.getSite();
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
    }

    @Test(groups = {"oneSite"})
    private void test() throws Exception {
        ScmConfigProperties.Builder builder = ScmConfigProperties.builder();
        //builder set instance
        List<NodeWrapper> nodes = site.getNodes(site.getNodeNum());
        for (NodeWrapper node : nodes) {
            builder.instance(node.getUrl());
        }
        //update configuration and check results
        ScmSession session = null;
        try {
            session = TestScmTools.createSession(site);
            ScmConfigProperties confProp = builder
                    .updateProperty(ConfigCommonDefind.scm_audit_mask, "ALL")
                    .updateProperty(ConfigCommonDefind.scm_audit_userMask, "LOCAL")
                    .build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration.setConfigProperties(session, confProp);
            List<String> expServiceNames = new ArrayList<String>();
            expServiceNames.add(site.getSiteServiceName());
            ConfUtil.checkResultSet(actResults, site.getNodeNum(), 0, expServiceNames, new ArrayList<String>());
        } finally {
            if (session != null) {
                session.close();
            }
        }
        //check updated configuration take effect
        ConfUtil.checkTakeEffect(site, fileName);
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
    }
}
