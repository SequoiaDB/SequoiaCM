package com.sequoiacm.config;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @author fanyu
 * @Description: SCM-2312 :: cmConfigProperties.build().updateInstance()参数校验
 * @Date:2018年12月10日
 * @version:1.0
 */
public class Param_UpdateInstance2312 extends TestScmBase {
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
    }

    // SEQUOIACM-403
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNull() {
        try {
            ScmConfigProperties.builder().instance( null )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .build();
            Assert.fail(
                    " ScmConfigProperties.builder().instance(null) must be "
                            + "failed when the instances is null" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    // SEQUOIACM-403
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHybrid() {
        try {
            List< String > list = new ArrayList< String >();
            list.add( site.getNode().getUrl() );
            ScmConfigProperties.builder().instances( list ).instance( null )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .build();
            Assert.fail(
                    " ScmConfigProperties.builder().instance(null)  must be "
                            + "failed when the instances  is 0" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
    }
}
