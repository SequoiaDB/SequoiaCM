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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fanyu
 * @Description: SCM-2297 :: 并发修改不同服务的节点配置
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateConf2297 extends TestScmBase {
    private String fileName = "file2297";
    private List<SiteWrapper> updateSites = new ArrayList<SiteWrapper>();
    private List<SiteWrapper> initSites = new ArrayList<SiteWrapper>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        List<SiteWrapper> sites = ScmInfo.getAllSites();
        for (SiteWrapper site : sites) {
            ConfUtil.deleteAuditConf(site.getSiteServiceName());
        }
        updateSites.addAll(sites.subList(0, 2));
        initSites.addAll(sites.subList(2, sites.size()));
    }

    @Test(groups = {"fourSite"})
    private void test() throws Exception {
        List<Update> threads = new ArrayList<Update>();
        for (SiteWrapper site : updateSites) {
            threads.add(new Update(site));
        }

        for (Update thread : threads) {
            thread.start();
        }

        for (Update thread : threads) {
            Assert.assertTrue(thread.isSuccess(), thread.getErrorMsg());
        }

        //check local configuration
        Map<String,String> map = new HashMap<String,String>();
        map.put(ConfigCommonDefind.scm_audit_mask, "ALL");
        map.put(ConfigCommonDefind.scm_audit_userMask, "LOCAL");
        for(SiteWrapper site :updateSites) {
            for (NodeWrapper node : site.getNodes(site.getNodeNum())) {
                ConfUtil.checkUpdatedConf(node.getUrl(), map);
            }
        }

        //check updated configuration take effect
        for (SiteWrapper site : updateSites) {
            ConfUtil.checkTakeEffect(site, fileName);
        }

        //check othser service updated configure do not take effect
        for (SiteWrapper site : initSites) {
            ConfUtil.checkNotTakeEffect(site, fileName);
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        for (SiteWrapper site : updateSites) {
            ConfUtil.deleteAuditConf(site.getSiteServiceName());
        }
    }

    private class Update extends TestThreadBase {
        private SiteWrapper site = null;

        public Update(SiteWrapper site) {
            this.site = site;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession(site);
                ScmConfigProperties confProp = ScmConfigProperties.builder()
                        .service(site.getSiteServiceName())
                        .updateProperty(ConfigCommonDefind.scm_audit_mask, "ALL")
                        .updateProperty(ConfigCommonDefind.scm_audit_userMask, "LOCAL")
                        .build();
               ScmUpdateConfResultSet result =  ScmSystem.Configuration.setConfigProperties(session, confProp);
               System.out.println("result = " + result.toString());
            } finally {
                if (session != null) {
                    session.close();
                }
            }
        }
    }
}
