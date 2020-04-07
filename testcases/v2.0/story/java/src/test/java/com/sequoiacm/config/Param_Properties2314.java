package com.sequoiacm.config;

import java.util.HashMap;
import java.util.Map;

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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @author fanyu
 * @Description: SCM-2314:ScmConfigProperties.build().properties()参数校验
 * @Date:2018年12月10日
 * @version:1.0
 */
public class Param_Properties2314 extends TestScmBase {
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNull() {
        try {
            Map< String, String > conf = new HashMap< String, String >();
            conf.put( ConfigCommonDefind.scm_audit_mask, "ALL" );
            ScmConfigProperties.builder()
                    .service( site.getSiteServiceName() )
                    .updateProperties( conf )
                    .updateProperties( null )
                    .build();
            Assert.fail(
                    " ScmConfigProperties.builder().properties(null) must be " +
                            "failed when the properties is null" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testEmpty() {
        try {
            ScmConfigProperties.builder()
                    .service( site.getSiteServiceName() )
                    .updateProperties( new HashMap< String, String >() )
                    .build();
            Assert.fail(
                    " ScmConfigProperties.builder().properties(null) must be " +
                            "failed when the properties is null" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHybrid() throws Exception {
        testHybrid1();
        testHybrid2();
    }

    private void testHybrid1() throws Exception {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            ScmConfigProperties conf = ScmConfigProperties.builder()
                    .service( site.getSiteServiceName() )
                    .updateProperties( new HashMap< String, String >() )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .build();
            ScmUpdateConfResultSet set = ScmSystem.Configuration
                    .setConfigProperties( session, conf );
            ConfUtil.checkTakeEffect( site, "file2314" );
        } finally {
            ConfUtil.deleteAuditConf( site.getSiteServiceName() );
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void testHybrid2() throws ScmException {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            ScmConfigProperties conf = ScmConfigProperties.builder()
                    .service( site.getSiteServiceName() )
                    .updateProperties( new HashMap< String, String >() )
                    .updateProperty( "", "" )
                    .build();
            ScmUpdateConfResultSet set = ScmSystem.Configuration
                    .setConfigProperties( session, conf );
            Assert.assertTrue( set.getFailures().size() <= site.getNodeNum(),
                    set.toString() );
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
