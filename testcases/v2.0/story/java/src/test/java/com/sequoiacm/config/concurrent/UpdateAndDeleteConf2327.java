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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @Description: SCM-2327 :: 并发删除和更新不同的配置
 * @author fanyu
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateAndDeleteConf2327 extends TestScmBase {
    private String fileName = "file2327";
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        Update uThraed = new Update();
        Delete dThread = new Delete();
        uThraed.start();
        dThread.start();
        Assert.assertEquals( uThraed.isSuccess(), true, uThraed.getErrorMsg() );
        Assert.assertEquals( dThread.isSuccess(), true, dThread.getErrorMsg() );

        update();
        ConfUtil.checkTakeEffect( site, fileName );
        Map< String, String > map = new HashMap< String, String >();
        map.put( ConfigCommonDefind.scm_audit_mask, "ALL" );
        map.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        for ( NodeWrapper node : site.getNodes( site.getNodeNum() ) ) {
            ConfUtil.checkUpdatedConf( node.getUrl(), map );
        }
        ConfUtil.checkTakeEffect( site, fileName );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    private void update() {
        ScmSession session = null;
        ScmUpdateConfResultSet actResult = null;
        try {
            session = ScmSessionUtils.createSession( site );
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .service( site.getSiteServiceName() )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();
            actResult = ScmSystem.Configuration.setConfigProperties( session,
                    confProp );
            List< String > expServiceNames = new ArrayList< String >();
            expServiceNames.add( site.getSiteServiceName() );
            ConfUtil.checkResultSet( actResult, site.getNodeNum(), 0,
                    expServiceNames, new ArrayList< String >() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            if ( actResult != null ) {
                Assert.fail( e.getMessage() + ",actResult = "
                        + actResult.toString() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class Update extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmUpdateConfResultSet actResults = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmConfigProperties confProp = ScmConfigProperties.builder()
                        .service( site.getSiteServiceName() )
                        .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                                "LOCAL" )
                        .build();
                actResults = ScmSystem.Configuration
                        .setConfigProperties( session, confProp );
                List< String > expServiceNames = new ArrayList< String >();
                expServiceNames.add( site.getSiteServiceName() );
                ConfUtil.checkResultSet( actResults, site.getNodeNum(), 0,
                        expServiceNames, new ArrayList< String >() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class Delete extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmUpdateConfResultSet actResult = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmConfigProperties confProp = ScmConfigProperties.builder()
                        .service( site.getSiteServiceName() )
                        .deleteProperty( ConfigCommonDefind.scm_audit_mask )
                        .build();
                actResult = ScmSystem.Configuration
                        .setConfigProperties( session, confProp );
                List< String > expServiceNames = new ArrayList< String >();
                expServiceNames.add( site.getSiteServiceName() );
                ConfUtil.checkResultSet( actResult, site.getNodeNum(), 0,
                        expServiceNames, new ArrayList< String >() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
