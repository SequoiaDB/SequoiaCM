package com.sequoiacm.config.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.testcommon.NodeWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @author fanyu
 * @Description: SCM-2296 :: 并发修改同一服务的节点配置
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateConf2296 extends TestScmBase {
    private String fileName = "file2296";
    private SiteWrapper site = null;
    private List< NodeWrapper > nodes = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        site = ScmInfo.getSite();
        nodes = site.getNodes( site.getNodeNum() );
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        List< Update > threads = new ArrayList< Update >();
        for ( NodeWrapper node : nodes ) {
            threads.add( new Update( node.getUrl() ) );
        }

        for ( Update thread : threads ) {
            thread.start( 5 );
        }

        for ( Update thread : threads ) {
            Assert.assertTrue( thread.isSuccess(), thread.getErrorMsg() );
        }

        //check local configuration
        Map< String, String > map = new HashMap< String, String >();
        map.put( ConfigCommonDefind.scm_audit_mask, "ALL" );
        map.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        for ( NodeWrapper node : nodes ) {
            ConfUtil.checkUpdatedConf( node.getUrl(), map );
        }

        //check updated configuration take effect
        ConfUtil.checkTakeEffect( site, fileName );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    private class Update extends TestThreadBase {
        private String url = null;

        public Update( String url ) {
            this.url = url;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmConfigProperties confProp = ScmConfigProperties.builder()
                        .instance( url )
                        .updateProperty( ConfigCommonDefind.scm_audit_mask,
                                "ALL" )
                        .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                                "LOCAL" )
                        .build();
                ScmUpdateConfResultSet results = ScmSystem.Configuration
                        .setConfigProperties( session, confProp );
                System.out.println( "results = " + results.toString() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
