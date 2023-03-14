package com.sequoiacm.config;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;

/**
 * @author fanyu
 * @Description: SCM-2313 :: ScmConfigProperties.build().property参数校验
 * @Date:2018年12月10日
 * @version:1.0
 */
public class Param_Property2313 extends TestScmBase {
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testKeyIsNull() {
        try {
            ScmConfigProperties.builder().service( site.getSiteServiceName() )
                    .updateProperty( null, "ALL" ).build();
            Assert.fail(
                    " ScmConfigProperties.builder().property(null,\"ALL\") "
                            + "must be failed when the key is null" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testValueIsNull() {
        try {
            ScmConfigProperties.builder().service( site.getSiteServiceName() )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, null )
                    .build();
            Assert.fail( " ScmConfigProperties.builder().property"
                    + "(ConfigCommonDefind.scm_audit_mask, null) must "
                    + "be failed when the value is null" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testKeyIsEmptyStr() throws ScmException {
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( site );
            ScmConfigProperties conf = ScmConfigProperties.builder()
                    .service( site.getSiteServiceName() )
                    .updateProperty( "", ConfigCommonDefind.scm_audit_mask )
                    .build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration
                    .setConfigProperties( session, conf );
            Assert.assertEquals( actResults.getFailures().size(),
                    site.getNodeNum() );
            Assert.assertEquals(
                    actResults.getFailures().get( 0 ).getServiceName(),
                    site.getSiteServiceName().toUpperCase() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
    }
}
