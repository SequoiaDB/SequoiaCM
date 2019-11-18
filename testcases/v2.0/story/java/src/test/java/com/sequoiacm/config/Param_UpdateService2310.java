package com.sequoiacm.config;

import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author fanyu
 * @Description: SCM-2309 :: ScmConfigProperties.build().updateServices()参数校验
 * @Date:2018年12月10日
 * @version:1.0
 */
public class Param_UpdateService2310 extends TestScmBase {
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
    }

    //http://jira:8080/browse/SEQUOIACM-403
    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void testNull() {
        try {
            ScmConfigProperties.builder()
                    .service(null)
                    .updateProperty(ConfigCommonDefind.scm_audit_mask, "ALL")
                    .build();
            Assert.fail(" ScmConfigProperties.builder().service must be failed when services is null");
        } catch (ScmException e) {
            if (e.getError() != ScmError.INVALID_ARGUMENT) {
                Assert.fail(e.getMessage());
            }
        }
    }

    //http://jira:8080/browse/SEQUOIACM-403
    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void testNull1() {
        try {
            ScmConfigProperties.builder()
                    .service(null)
                    .service(site.getSiteServiceName())
                    .updateProperty(ConfigCommonDefind.scm_audit_mask, "ALL")
                    .build();
            Assert.fail(" ScmConfigProperties.builder().service must be failed when the service contains null");
        } catch (ScmException e) {
            if (e.getError() != ScmError.INVALID_ARGUMENT) {
                Assert.fail(e.getMessage());
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
    }
}
