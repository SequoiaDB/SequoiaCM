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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @author fanyu
 * @Description: SCM-2290 :: 指定acceptUnknownProperties为true，修改配置
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateServiceConf2290 extends TestScmBase {
    private String fileName = "file2290";
    private SiteWrapper updatedSite = null;
    private SiteWrapper initSite = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        List< SiteWrapper > siteList = ScmInfo.getAllSites();
        updatedSite = siteList.get( 1 );
        initSite = siteList.get( 0 );
        ConfUtil.deleteAuditConf( updatedSite.getSiteServiceName() );
        ConfUtil.deleteAuditConf( initSite.getSiteServiceName() );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        testInvalidParam();
        testRightParam();
    }

    private void testInvalidParam() throws ScmException, InterruptedException {
        //update configuration and check results
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( updatedSite );
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .acceptUnknownProperties( true )
                    .service( updatedSite.getSiteServiceName() )
                    .updateProperty( "scm1.audit.mask", "ALL" )
                    .updateProperty( "scm1.audit.userMask", "LOCAL" ).build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            List< String > expServiceNames = new ArrayList< String >();
            expServiceNames.add( updatedSite.getSiteServiceName() );
            ConfUtil.checkResultSet( actResults, updatedSite.getNodeNum(), 0,
                    expServiceNames, new ArrayList< String >() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        //check updated configuration do not take effect
        ConfUtil.checkNotTakeEffect( updatedSite, fileName );
        //restore
        ConfUtil.deleteAuditConf( updatedSite.getSiteServiceName() );
    }

    private void testRightParam() throws Exception {
        //update configuration and check results
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( updatedSite );
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .acceptUnknownProperties( true )
                    .service( updatedSite.getSiteServiceName() )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask,
                            "FILE_DML" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" ).build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            List< String > expServiceNames = new ArrayList< String >();
            expServiceNames.add( updatedSite.getSiteServiceName() );
            ConfUtil.checkResultSet( actResults, updatedSite.getNodeNum(), 0,
                    expServiceNames, new ArrayList< String >() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        //check updated configuration take effect
        ConfUtil.checkTakeEffect( updatedSite, fileName );
        //check otherservice's configration is not updated
        ConfUtil.checkNotTakeEffect( initSite, fileName );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf( updatedSite.getSiteServiceName() );
        ConfUtil.deleteAuditConf( initSite.getSiteServiceName() );
    }
}
