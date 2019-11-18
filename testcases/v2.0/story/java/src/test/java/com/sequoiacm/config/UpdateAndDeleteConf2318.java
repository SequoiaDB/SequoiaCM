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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fanyu
 * @Description:  SCM-2318 :: 更新和删除不同的配置项
 * @Date:2018年12月17日
 * @version:1.0
 */
public class UpdateAndDeleteConf2318 extends TestScmBase {
    private String fileName = "file2318";
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        site = ScmInfo.getSite();
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
    }

    @Test
    private void test() throws Exception {
        ScmSession session = null;
        try {
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .service(site.getSiteServiceName())
                    .updateProperty(ConfigCommonDefind.scm_audit_userMask,"LOCAL")
                    .deleteProperty(ConfigCommonDefind.scm_audit_mask)
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

        Map<String,String> map = new HashMap<String,String>();
        map.put(ConfigCommonDefind.scm_audit_userMask,"LOCAL");

        List<String> list = new ArrayList<String>();
        list.add(ConfigCommonDefind.scm_audit_mask);

        for (NodeWrapper node : site.getNodes(site.getNodeNum())) {
            ConfUtil.checkUpdatedConf(node.getUrl(),map);
            ConfUtil.checkDeletedConf(node.getUrl(),list);
        }

        //update
        ScmSession session1 = null;
        try {
            session1 = TestScmTools.createSession(site);
            ScmSystem.Configuration.setConfigProperties(session1, ScmConfigProperties.builder()
                    .service(site.getSiteServiceName())
                    .updateProperty(ConfigCommonDefind.scm_audit_mask, "ALL").build());
        }finally{
            if(session1 != null){
                session1.close();
            }
        }

        ConfUtil.checkTakeEffect(site, fileName);
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf(site.getSiteServiceName());
    }
}
