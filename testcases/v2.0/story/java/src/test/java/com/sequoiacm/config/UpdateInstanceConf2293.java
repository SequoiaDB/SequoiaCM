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
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @Description: SCM-2293:指定不存在的实例，修改配置
 * @author fanyu
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateInstanceConf2293 extends TestScmBase {
    private String fileName = "file2293";
    private SiteWrapper site = null;
    private ScmSession session = null;
    private List< NodeWrapper > nodes = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        nodes = site.getNodes( site.getNodeNum() );
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    // SEQUOIACM-1400暂时屏蔽
    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    private void testSingleInstance1() throws ScmException {
        try {
            ScmConfigProperties.Builder builder = ScmConfigProperties.builder();
            for ( NodeWrapper node : nodes ) {
                builder.instance( node.getUrl() );
            }
            ScmConfigProperties confProp = builder
                    .instance( site.getNode().getHost() + "::"
                            + site.getNode().getPort() )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            Assert.fail(
                    "update configuration should be failed when node's url is"
                            + " invalid,actResults = "
                            + actResults.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }
        ConfUtil.checkNotTakeEffect( site, fileName );
    }

    // SEQUOIACM-1400暂时屏蔽
    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    private void testSingleInstance2() throws ScmException {
        try {
            ScmConfigProperties.Builder builder = ScmConfigProperties.builder();
            for ( NodeWrapper node : nodes ) {
                builder.instance( node.getUrl() );
            }
            ScmConfigProperties confProp = builder
                    .instance( site.getNode().getHost() + ":"
                            + ( site.getNode().getPort() - 1 ) )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            Assert.fail(
                    "update configuration should be failed when node's url is"
                            + " invalid,actResult = " + actResults.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }
        ConfUtil.checkNotTakeEffect( site, fileName );
    }

    // SEQUOIACM-1400暂时屏蔽
    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    private void testListInstance2() throws ScmException {
        try {
            List< String > instances = new ArrayList< String >();
            for ( NodeWrapper node : nodes ) {
                instances.add( node.getUrl() );
            }
            instances.add( site.getNode().getHost() + ":"
                    + ( site.getNode().getPort() - 1 ) );
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .instances( instances )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            Assert.fail( "update configuration should be failed when instances "
                    + "contains invalid,actResults = "
                    + actResults.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }
        ConfUtil.checkNotTakeEffect( site, fileName );
    }

    // SEQUOIACM-1400暂时屏蔽
    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    private void testListInstance1() throws ScmException {
        try {
            List< String > instances = new ArrayList< String >();
            for ( NodeWrapper node : nodes ) {
                instances.add( node.getUrl() );
            }
            instances.add( site.getNode().getHost() + "&:00" );
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .instances( instances )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();
            ScmUpdateConfResultSet actResults = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            Assert.fail(
                    "update configuration should be failed when instances contains invalid,actResults = "
                            + actResults.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                throw e;
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
