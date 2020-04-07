package com.sequoiacm.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.NodeWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @author fanyu
 * @Description: SCM-2288 :: 指定动态生效类型为服务列表，修改配置
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateServicesConf2288 extends TestScmBase {
    private String fileName = "file2288";
    private List< SiteWrapper > siteList = null;
    private List< SiteWrapper > updatedSites = new ArrayList< SiteWrapper >();
    private List< SiteWrapper > initdSites = new ArrayList< SiteWrapper >();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        siteList = ScmInfo.getAllSites();
        updatedSites.addAll( siteList.subList( 0, 2 ) );
        initdSites.addAll( siteList.subList( 2, siteList.size() ) );
        for ( SiteWrapper site : siteList ) {
            ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        //update configuration and check results
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( updatedSites.get( 0 ) );
            int expOkNum = 0;
            ScmConfigProperties.Builder builder = ScmConfigProperties.builder();
            List< String > serviceNames = new ArrayList< String >();
            for ( SiteWrapper site : updatedSites ) {
                serviceNames.add( site.getSiteServiceName() );
                expOkNum += site.getNodeNum();
            }
            ScmConfigProperties confProp = builder
                    .services( serviceNames )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask,
                            "FILE_DML" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            List< String > expServiceNames = new ArrayList< String >();
            for ( SiteWrapper site : updatedSites ) {
                expServiceNames.add( site.getSiteServiceName() );
            }
            ConfUtil.checkResultSet( actResults, expOkNum, 0, expServiceNames,
                    new ArrayList< String >() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }

        //check updated configuration take effect
        Map< String, String > map = new HashMap< String, String >();
        map.put( ConfigCommonDefind.scm_audit_userMask, "FILE_DML" );
        map.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );

        for ( SiteWrapper updatedSite : updatedSites ) {
            ConfUtil.checkTakeEffect( updatedSite, fileName );
            for ( NodeWrapper node : updatedSite
                    .getNodes( updatedSite.getNodeNum() ) ) {
                ConfUtil.checkUpdatedConf( node.getUrl(), map );
            }
        }

        //check otherservice's configration is not updated
        Map< String, String > map1 = new HashMap< String, String >();
        map.put( ConfigCommonDefind.scm_audit_mask, "ALL" );
        map.put( ConfigCommonDefind.scm_audit_userMask, "TOKEN" );
        for ( SiteWrapper initSite : initdSites ) {
            ConfUtil.checkNotTakeEffect( initSite, fileName );
            for ( NodeWrapper node : initSite
                    .getNodes( initSite.getNodeNum() ) ) {
                ConfUtil.checkUpdatedConf( node.getUrl(), map1 );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        for ( SiteWrapper site : siteList ) {
            ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        }
    }
}
