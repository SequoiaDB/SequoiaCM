package com.sequoiacm.config;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @author fanyu
 * @Description: SCM-2295 :: 多次修改配置
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateConfManyTimes2295 extends TestScmBase {
    private String fileName = "file2295";
    private SiteWrapper updatedSite = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        updatedSite = ScmInfo.getRootSite();
        ConfUtil.deleteAuditConf( updatedSite.getSiteServiceName() );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        testSame();
        testDiff();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf( updatedSite.getSiteServiceName() );
    }

    private void testSame() throws Exception {
        // update configuration and check results
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( updatedSite );
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .acceptUnknownProperties( true )
                    .service( updatedSite.getSiteServiceName() )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();
            ScmUpdateConfResultSet actResults = null;
            for ( int i = 0; i < 3; i++ ) {
                actResults = ScmSystem.Configuration
                        .setConfigProperties( session, confProp );
            }
            List< String > expServiceNames = new ArrayList< String >();
            expServiceNames.add( updatedSite.getSiteServiceName() );
            ConfUtil.checkResultSet( actResults, updatedSite.getNodeNum(), 0,
                    expServiceNames, new ArrayList< String >() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void testDiff() throws Exception {
        // update configuration and check results
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( updatedSite );
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .acceptUnknownProperties( true )
                    .service( updatedSite.getSiteServiceName() )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();

            ScmConfigProperties confProp1 = ScmConfigProperties.builder()
                    .acceptUnknownProperties( true )
                    .service( updatedSite.getSiteServiceName() )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask,
                            "FILE_DML" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();

            ScmUpdateConfResultSet actResults = null;
            for ( int i = 0; i < 10; i++ ) {
                if ( i % 2 == 0 ) {
                    actResults = ScmSystem.Configuration
                            .setConfigProperties( session, confProp );
                } else {
                    actResults = ScmSystem.Configuration
                            .setConfigProperties( session, confProp1 );
                }
            }
            List< String > expServiceNames = new ArrayList< String >();
            expServiceNames.add( updatedSite.getSiteServiceName() );
            ConfUtil.checkResultSet( actResults, updatedSite.getNodeNum(), 0,
                    expServiceNames, new ArrayList< String >() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        ConfUtil.checkTakeEffect( updatedSite, fileName );
    }
}
