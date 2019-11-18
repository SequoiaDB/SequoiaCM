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
 * @Description:  SCM-2322 :: ScmConfigProperties.Builder.deleteProperty参数校验
 * @Date:2018年12月17日
 * @version:1.0
 */
public class Param_DeleteProperty2322 extends TestScmBase {
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void testKeyIsNull() {
        try {
            ScmConfigProperties.builder()
                    .service(site.getSiteServiceName())
                    .deleteProperty(null)
                    .build();
            Assert.fail(" ScmConfigProperties.builder().deleteProperty(null) must be failed when the key is null");
        } catch (ScmException e) {
            if (e.getError() != ScmError.INVALID_ARGUMENT) {
                Assert.fail(e.getMessage());
            }
        }
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void testHybrid() {
        try {
            ScmConfigProperties.builder()
                    .service(site.getSiteServiceName())
                    .updateProperty(ConfigCommonDefind.scm_audit_mask, "ALL")
                    .deleteProperty(null)
                    .build();
            Assert.fail(" ScmConfigProperties.builder().deleteProperty(null) must be failed when the value is null");
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
