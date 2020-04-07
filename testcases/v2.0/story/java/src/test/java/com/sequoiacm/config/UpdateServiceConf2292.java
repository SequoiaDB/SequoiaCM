package com.sequoiacm.config;

import java.util.ArrayList;
import java.util.List;

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
 * @Description: SCM-2292 :: 指定不存在的服务，修改配置
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateServiceConf2292 extends TestScmBase {
    private String fileName = "file2292";
    private SiteWrapper site = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testSingleService() throws ScmException {
        try {
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .service( site.getSiteServiceName() + "_inexistence" )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" ).build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            Assert.fail(
                    "update configuration should be failed when servicename " +
                            "is invalid,actResults = " +
                            actResults.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                Assert.fail( e.getMessage() );
            }
        }
        ConfUtil.checkNotTakeEffect( site, fileName );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testListService() throws ScmException {
        try {
            List< String > serviceList = new ArrayList< String >();
            serviceList.add( site.getSiteServiceName() );
            serviceList.add( site.getSiteServiceName() + "_inexistence" );
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .services( serviceList )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" ).build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            Assert.fail(
                    "update configuration should be failed when servicenames " +
                            "contains invalid,actResults = " +
                            actResults.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                Assert.fail( e.getMessage() );
            }
        }
        ConfUtil.checkNotTakeEffect( site, fileName );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
    }
}
