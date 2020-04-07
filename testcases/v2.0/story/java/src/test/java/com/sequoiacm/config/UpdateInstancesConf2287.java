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
import com.sequoiacm.testcommon.NodeWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @author fanyu
 * @Description: SCM-2287 :: 指定动态生效类型为实例列表，修改配置
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateInstancesConf2287 extends TestScmBase {
    private String fileName = "file2287";
    private List< SiteWrapper > siteList = null;
    private List< NodeWrapper > nodeList = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        siteList = ScmInfo.getAllSites();
        nodeList = ScmInfo.getAllNodes();
        for ( SiteWrapper site : siteList ) {
            ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        List< String > instances = new ArrayList< String >();
        for ( NodeWrapper node : nodeList ) {
            instances.add( node.getUrl() );
        }
        //update configuration and check results
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( siteList.get( 0 ) );
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .instances( instances )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            List< String > expServiceNames = new ArrayList< String >();
            for ( SiteWrapper site : siteList ) {
                expServiceNames.add( site.getSiteServiceName() );
            }
            ConfUtil.checkResultSet( actResults, nodeList.size(), 0,
                    expServiceNames, new ArrayList< String >() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        //check updated configuration take effect
        for ( SiteWrapper site : siteList ) {
            ConfUtil.checkTakeEffect( site, fileName );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        for ( SiteWrapper site : siteList ) {
            ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        }
    }
}
