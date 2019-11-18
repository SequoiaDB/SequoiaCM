package com.sequoiacm.config;

import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fanyu
 * @Description: SCM-2332:ScmConfigProperties参数校验
 * @Date:2018年12月04日
 * @version:1.0
 */
public class Param_ScmConfigProperties2332 extends TestScmBase {
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
    }

    @Test(groups = {"oneSite","twoSite", "fourSite"})
    private void test() throws ScmException {
        ScmConfigProperties confProp = ScmConfigProperties.builder()
                .service(site.getSiteServiceName())
                .acceptUnknownProperties(true)
                .updateProperty(ConfigCommonDefind.scm_audit_mask, "ALL")
                .updateProperty(ConfigCommonDefind.scm_audit_userMask, "LOCAL")
                .deleteProperty(ConfigCommonDefind.scm_audit_mask)
                .deleteProperty(ConfigCommonDefind.scm_audit_userMask)
                .build();
        Assert.assertEquals(confProp.getTargetType(),"SERVICE");
        Assert.assertEquals( confProp.getTargets().get(0),site.getSiteServiceName());
        Assert.assertEquals( confProp.isAcceptUnknownProps(),true);
        Assert.assertEquals( confProp.getDeleteProps().get(0),ConfigCommonDefind.scm_audit_mask);
        Assert.assertEquals( confProp.getDeleteProps().get(1),ConfigCommonDefind.scm_audit_userMask);
        Map<String,String> map = new HashMap<String,String>();
        map.put(ConfigCommonDefind.scm_audit_mask, "ALL");
        map.put(ConfigCommonDefind.scm_audit_userMask, "LOCAL");
        Assert.assertEquals(confProp.getUpdateProps(),map);
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
    }
}
